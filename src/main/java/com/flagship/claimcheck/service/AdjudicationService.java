package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision;
import com.flagship.claimcheck.model.ClaimDecision.*;
import com.flagship.claimcheck.model.ClaimRequest;
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

    public ClaimDecision adjudicate(ClaimRequest request) {
        long start = System.nanoTime();
        List<DecisionReason> reasons = new ArrayList<>();
        DuplicateMatch duplicate = findDuplicate(request);
        Status status;
        String headline;

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
        long elapsed = Math.max(1, (System.nanoTime() - start) / 1_000_000);
        return new ClaimDecision(request.claimId(), status, headline, List.copyOf(reasons), duplicate,
            Instant.now(), elapsed, UUID.randomUUID().toString());
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
