package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision;
import com.flagship.claimcheck.model.ClaimDecision.*;
import com.flagship.claimcheck.model.ClaimRequest;
import com.flagship.claimcheck.model.DecisionAuditRecord;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;

@Service
public class AdjudicationService {
    public static final String RULE_SET_VERSION = "2026.07.1";
    static final String ELIGIBILITY_RULE = "ELIGIBILITY.BASIC.V1";
    static final String DUPLICATE_RULE = "DUPLICATE.EXACT.V1";
    static final String AMOUNT_RULE = "AMOUNT.AUTO_APPROVAL.V1";
    private static final Logger log = LoggerFactory.getLogger(AdjudicationService.class);
    private static final BigDecimal REVIEW_LIMIT = new BigDecimal("5000.00");

    private final List<LegacyClaim> legacyClaims = List.of(
        new LegacyClaim("CLM-902184", "MBR-10482", "PRV-4481", "99213", LocalDate.of(2026, 7, 18), new BigDecimal("185.00")),
        new LegacyClaim("CLM-775091", "MBR-22019", "PRV-2204", "70553", LocalDate.of(2026, 7, 2), new BigDecimal("2400.00")),
        new LegacyClaim("CLM-881426", "MBR-10482", "PRV-4481", "80053", LocalDate.of(2026, 6, 21), new BigDecimal("96.40"))
    );
    private final DecisionAuditRepository repository;
    private final MeterRegistry meters;
    private final ObservationRegistry observations;
    private final Tracer tracer;

    @Autowired
    public AdjudicationService(DecisionAuditRepository repository, MeterRegistry meters,
                               ObservationRegistry observations, Optional<Tracer> tracer) {
        this.repository = repository;
        this.meters = meters;
        this.observations = observations;
        this.tracer = tracer.orElse(null);
    }

    /** Convenience constructor for callers outside the Spring container. */
    public AdjudicationService() {
        this(decision -> decision, Metrics.globalRegistry, ObservationRegistry.NOOP, Optional.empty());
    }

    public ClaimDecision adjudicate(ClaimRequest request) {
        return adjudicate(request, UUID.randomUUID().toString());
    }

    public ClaimDecision adjudicate(ClaimRequest request, String correlationId) {
        Instant receivedAt = Instant.now();
        Timer.Sample latency = Timer.start(meters);
        String fingerprint = fingerprint(request);
        List<String> evaluatedRules = new ArrayList<>();
        try {
            evaluateEligibility(evaluatedRules);
            DuplicateMatch duplicate = queryDuplicate(request, evaluatedRules);
            evaluatedRules.add(AMOUNT_RULE);
            Status status;
            String headline;
            List<DecisionReason> reasons = new ArrayList<>();
            if (duplicate != null) {
                status = Status.DENIED;
                headline = "Potential duplicate detected";
                reasons.add(new DecisionReason("DUP-001", "This claim matches a previously submitted claim.", Severity.ERROR));
            } else if (request.amount().compareTo(REVIEW_LIMIT) > 0) {
                status = Status.REVIEW;
                headline = "Manual review required";
                reasons.add(new DecisionReason("AMT-101", "Claim amount exceeds the $5,000 auto-approval threshold.", Severity.WARNING));
            } else {
                status = Status.APPROVED;
                headline = "Claim passed pre-adjudication";
                reasons.add(new DecisionReason("ELG-000", "Member and claim details passed basic eligibility checks.", Severity.INFO));
                reasons.add(new DecisionReason("DUP-000", "No matching claim was found in the legacy claims index.", Severity.INFO));
            }

            Instant evaluatedAt = Instant.now();
            List<String> references = duplicate == null ? List.of() : List.of(duplicate.claimId());
            String traceId = currentTraceId(correlationId);
            DecisionAuditRecord stored = persist(new DecisionAuditRecord(correlationId, RULE_SET_VERSION, fingerprint,
                status, reasons.stream().map(DecisionReason::code).toList(), List.copyOf(evaluatedRules), references,
                receivedAt, evaluatedAt, Instant.now(), traceId));
            recordDecisionMetrics(status, reasons);
            long elapsed = Math.max(1, Duration.between(receivedAt, stored.persistedAt()).toMillis());
            log.atInfo().addKeyValue("event", "claim_decided").addKeyValue("correlationId", correlationId)
                .addKeyValue("ruleSetVersion", RULE_SET_VERSION).addKeyValue("inputFingerprint", fingerprint)
                .addKeyValue("outcome", status).addKeyValue("reasonCodes", stored.reasonCodes())
                .log("Claim adjudication completed");
            return new ClaimDecision(request.claimId(), status, headline, List.copyOf(reasons), duplicate,
                RULE_SET_VERSION, fingerprint, List.copyOf(evaluatedRules), references,
                new DecisionTimestamps(receivedAt, evaluatedAt, stored.persistedAt()), elapsed, correlationId, traceId);
        } catch (RuntimeException error) {
            meters.counter("claim.adjudication.errors", "operation", "adjudicate").increment();
            log.atError().setCause(error).addKeyValue("correlationId", correlationId)
                .addKeyValue("inputFingerprint", fingerprint).log("Claim adjudication failed");
            throw error;
        } finally {
            latency.stop(meters.timer("claim.adjudication.latency"));
        }
    }

    private void evaluateEligibility(List<String> evaluatedRules) {
        Observation.createNotStarted("claim.eligibility", observations).observe(() -> evaluatedRules.add(ELIGIBILITY_RULE));
    }

    private DuplicateMatch queryDuplicate(ClaimRequest request, List<String> evaluatedRules) {
        return Observation.createNotStarted("claim.duplicate.query", observations).observe(() -> {
            evaluatedRules.add(DUPLICATE_RULE);
            return legacyClaims.stream()
                .filter(c -> c.memberId.equals(request.memberId()) && c.providerId.equalsIgnoreCase(request.providerId())
                    && c.procedureCode.equalsIgnoreCase(request.procedureCode()) && c.serviceDate.equals(request.serviceDate())
                    && c.amount.compareTo(request.amount()) == 0)
                .findFirst().map(c -> new DuplicateMatch(c.claimId, "EXACT", 100, c.serviceDate.toString(), c.amount.toPlainString()))
                .orElse(null);
        });
    }

    private DecisionAuditRecord persist(DecisionAuditRecord record) {
        return Observation.createNotStarted("claim.decision.persistence", observations)
            .lowCardinalityKeyValue("operation", "save").observe(() -> repository.save(record));
    }

    private void recordDecisionMetrics(Status status, List<DecisionReason> reasons) {
        meters.counter("claim.decisions", "outcome", status.name()).increment();
        if (status == Status.REVIEW) meters.counter("claim.pended").increment();
        if (status == Status.DENIED) reasons.forEach(reason ->
            meters.counter("claim.denials", "reason", reason.code()).increment());
        // Registered even before shadow mode is enabled, so dashboards and alerts remain stable.
        meters.counter("claim.shadow.mismatches");
    }

    private String currentTraceId(String fallback) {
        return tracer != null && tracer.currentSpan() != null
            ? tracer.currentSpan().context().traceId() : fallback;
    }

    static String fingerprint(ClaimRequest request) {
        String canonical = String.join("|", request.claimId(), request.memberId(), request.providerId().toUpperCase(Locale.ROOT),
            request.procedureCode().toUpperCase(Locale.ROOT), request.serviceDate().toString(), request.amount().stripTrailingZeros().toPlainString());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private record LegacyClaim(String claimId, String memberId, String providerId, String procedureCode,
                               LocalDate serviceDate, BigDecimal amount) {}
}
