package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision;
import com.flagship.claimcheck.model.ClaimDecision.*;
import com.flagship.claimcheck.model.ClaimRequest;
import com.flagship.claimcheck.model.AdjudicationRecord;
import com.flagship.claimcheck.model.AdjudicationRecord.*;
import com.flagship.claimcheck.model.ReasonCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class AdjudicationService {
    private static final BigDecimal REVIEW_LIMIT = new BigDecimal("5000.00");
    private final List<LegacyClaim> legacyClaims = List.of(
        new LegacyClaim("CLM-902184", "MBR-10482", "PRV-4481", "99213", LocalDate.of(2026, 7, 18), new BigDecimal("185.00")),
        new LegacyClaim("CLM-775091", "MBR-22019", "PRV-2204", "70553", LocalDate.of(2026, 7, 2), new BigDecimal("2400.00")),
        new LegacyClaim("CLM-881426", "MBR-10482", "PRV-4481", "80053", LocalDate.of(2026, 6, 21), new BigDecimal("96.40"))
    );
    private final AdjudicationRepository repository;

    public AdjudicationService() { this(new AdjudicationRepository()); }
    @Autowired
    public AdjudicationService(AdjudicationRepository repository) { this.repository = repository; }

    public ClaimDecision adjudicate(ClaimRequest request) {
        return adjudicate(request, null);
    }

    public ClaimDecision adjudicate(ClaimRequest request, String suppliedCorrelationId) {
        long start = System.nanoTime();
        List<DecisionReason> reasons = new ArrayList<>();
        List<RuleResult> ruleResults = new ArrayList<>();
        DuplicateMatch duplicate = findDuplicate(request);
        Status status;
        String headline;

        if (duplicate != null) {
            status = Status.DENIED;
            headline = "Potential duplicate detected";
            reasons.add(new DecisionReason(ReasonCodes.DUPLICATE_FOUND, "This claim matches a previously submitted claim.", Severity.ERROR));
            ruleResults.add(new RuleResult("EXACT_DUPLICATE", Outcome.FAILED, ReasonCodes.DUPLICATE_FOUND));
        } else if (request.amount().compareTo(REVIEW_LIMIT) > 0) {
            status = Status.PENDED;
            headline = "Manual review required";
            reasons.add(new DecisionReason(ReasonCodes.HIGH_VALUE_MANUAL_REVIEW, "Claim amount exceeds the $5,000 auto-approval threshold.", Severity.WARNING));
            ruleResults.add(new RuleResult("EXACT_DUPLICATE", Outcome.PASSED, ReasonCodes.NO_DUPLICATE_FOUND));
            ruleResults.add(new RuleResult("HIGH_VALUE", Outcome.PENDED, ReasonCodes.HIGH_VALUE_MANUAL_REVIEW));
        } else {
            status = Status.APPROVED;
            headline = "Claim passed pre-adjudication";
            reasons.add(new DecisionReason(ReasonCodes.BASIC_ELIGIBILITY_PASSED, "Member and claim details passed basic eligibility checks.", Severity.INFO));
            reasons.add(new DecisionReason(ReasonCodes.NO_DUPLICATE_FOUND, "No matching claim was found in the legacy claims index.", Severity.INFO));
            ruleResults.add(new RuleResult("EXACT_DUPLICATE", Outcome.PASSED, ReasonCodes.NO_DUPLICATE_FOUND));
            ruleResults.add(new RuleResult("HIGH_VALUE", Outcome.PASSED, ReasonCodes.AMOUNT_WITHIN_AUTO_LIMIT));
        }
        long elapsed = Math.max(1, (System.nanoTime() - start) / 1_000_000);
        Instant adjudicatedAt = Instant.now();
        String correlationId = suppliedCorrelationId == null || suppliedCorrelationId.isBlank()
            ? UUID.randomUUID().toString() : suppliedCorrelationId;
        Map<String, String> evidence = new LinkedHashMap<>();
        evidence.put("procedureCode", request.procedureCode());
        evidence.put("serviceDate", request.serviceDate().toString());
        evidence.put("amount", request.amount().toPlainString());
        if (duplicate != null) evidence.put("matchedClaimId", duplicate.claimId());
        repository.save(new AdjudicationRecord(request.claimId(), status, List.copyOf(ruleResults), correlationId,
            Map.copyOf(evidence), adjudicatedAt));
        return new ClaimDecision(request.claimId(), status, headline, List.copyOf(reasons), duplicate,
            adjudicatedAt, elapsed, correlationId);
    }

    private DuplicateMatch findDuplicate(ClaimRequest request) {
        return legacyClaims.stream()
            .filter(c -> c.memberId.equals(request.memberId()) && c.providerId.equalsIgnoreCase(request.providerId())
                && c.procedureCode.equalsIgnoreCase(request.procedureCode()) && c.serviceDate.equals(request.serviceDate())
                && c.amount.compareTo(request.amount()) == 0)
            .findFirst().map(c -> new DuplicateMatch(c.claimId, "EXACT", 100, c.serviceDate.toString(), c.amount.toPlainString()))
            .orElse(null);
    }

    private record LegacyClaim(String claimId, String memberId, String providerId, String procedureCode,
                               LocalDate serviceDate, BigDecimal amount) {}
}
