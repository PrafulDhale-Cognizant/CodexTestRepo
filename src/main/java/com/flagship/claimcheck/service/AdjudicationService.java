package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision;
import com.flagship.claimcheck.model.ClaimDecision.DecisionReason;
import com.flagship.claimcheck.model.ClaimDecision.Severity;
import com.flagship.claimcheck.model.ClaimDecision.Status;
import com.flagship.claimcheck.model.ClaimRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Application service that orchestrates the independent claim domain rules. */
@Service
public class AdjudicationService {
    private final DuplicateDetectionService duplicates;
    private final EligibilityService eligibility;

    public AdjudicationService(DuplicateDetectionService duplicates, EligibilityService eligibility) {
        this.duplicates = duplicates;
        this.eligibility = eligibility;
    }

    public ClaimDecision adjudicate(ClaimRequest request, String requestedCorrelationId) {
        long start = System.nanoTime();
        String correlationId = requestedCorrelationId == null || requestedCorrelationId.isBlank()
            ? UUID.randomUUID().toString() : requestedCorrelationId;
        var duplicate = duplicates.findExactDuplicate(request).orElse(null);

        if (duplicate != null) {
            return decision(request, Status.DENIED, "Potential duplicate detected",
                List.of(new DecisionReason("DUP-001", "Claim matches a previously submitted claim.", Severity.ERROR)),
                duplicate, start, correlationId);
        }

        var eligibilityResult = eligibility.evaluate(request);
        if (!eligibilityResult.eligibleForAutomaticAdjudication()) {
            return decision(request, Status.PENDED, "Manual review required",
                List.of(new DecisionReason(eligibilityResult.reasonCode(), eligibilityResult.explanation(), Severity.WARNING)),
                null, start, correlationId);
        }

        return decision(request, Status.APPROVED, "Claim passed pre-adjudication", List.of(
            new DecisionReason(eligibilityResult.reasonCode(), eligibilityResult.explanation(), Severity.INFO),
            new DecisionReason("DUP-000", "No matching claim was found in the legacy claims index.", Severity.INFO)
        ), null, start, correlationId);
    }

    private ClaimDecision decision(ClaimRequest request, Status status, String headline,
                                   List<DecisionReason> reasons, ClaimDecision.DuplicateMatch duplicate,
                                   long start, String correlationId) {
        long elapsed = Math.max(1, (System.nanoTime() - start) / 1_000_000);
        return new ClaimDecision(request.claimId(), status, headline, reasons, duplicate,
            Instant.now(), elapsed, correlationId);
    }
}
