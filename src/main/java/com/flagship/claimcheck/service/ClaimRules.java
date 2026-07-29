package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision.*;
import com.flagship.claimcheck.model.ClaimRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class ClaimRules {
    private static final BigDecimal REVIEW_LIMIT = new BigDecimal("5000.00");
    private final List<LegacyClaim> legacyClaims = List.of(
        new LegacyClaim("CLM-902184", "MBR-10482", "PRV-4481", "99213", LocalDate.of(2026, 7, 18), new BigDecimal("185.00")),
        new LegacyClaim("CLM-775091", "MBR-22019", "PRV-2204", "70553", LocalDate.of(2026, 7, 2), new BigDecimal("2400.00")),
        new LegacyClaim("CLM-881426", "MBR-10482", "PRV-4481", "80053", LocalDate.of(2026, 6, 21), new BigDecimal("96.40"))
    );

    AdjudicationResult evaluate(ClaimRequest request) {
        List<DecisionReason> reasons = new ArrayList<>();
        DuplicateMatch duplicate = findDuplicate(request);
        if (duplicate != null) {
            reasons.add(new DecisionReason("DUP-001", "This claim matches a previously submitted claim.", Severity.ERROR));
            return new AdjudicationResult(Status.DENIED, "Potential duplicate detected", List.copyOf(reasons), duplicate);
        }
        if (request.amount().compareTo(REVIEW_LIMIT) > 0) {
            reasons.add(new DecisionReason("AMT-101", "Claim amount exceeds the $5,000 auto-approval threshold.", Severity.WARNING));
            return new AdjudicationResult(Status.REVIEW, "Manual review required", List.copyOf(reasons), null);
        }
        reasons.add(new DecisionReason("ELG-000", "Member and claim details passed basic eligibility checks.", Severity.INFO));
        reasons.add(new DecisionReason("DUP-000", "No matching claim was found in the legacy claims index.", Severity.INFO));
        return new AdjudicationResult(Status.APPROVED, "Claim passed pre-adjudication", List.copyOf(reasons), null);
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
