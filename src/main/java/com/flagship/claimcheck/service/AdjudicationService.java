package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision;
import com.flagship.claimcheck.model.ClaimDecision.*;
import com.flagship.claimcheck.model.ClaimRequest;
import com.flagship.claimcheck.domain.ClaimRecord;
import com.flagship.claimcheck.domain.DuplicateClaimService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class AdjudicationService {
    private static final BigDecimal REVIEW_LIMIT = new BigDecimal("5000.00");
    private final DuplicateClaimService duplicateClaimService;

    public AdjudicationService(DuplicateClaimService duplicateClaimService) {
        this.duplicateClaimService = duplicateClaimService;
    }

    public ClaimDecision adjudicate(ClaimRequest request) {
        long start = System.nanoTime();
        List<DecisionReason> reasons = new ArrayList<>();
        DuplicateMatch duplicate = findDuplicate(request);
        Status status;
        String headline;

        if (duplicate != null) {
            status = Status.DENIED;
            headline = "Potential duplicate detected";
            reasons.add(new DecisionReason(DuplicateClaimService.EXACT_MATCH_REASON,
                "This claim matches a previously submitted claim.", Severity.ERROR));
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
        var result = duplicateClaimService.findDuplicates(new ClaimRecord(request.claimId(), request.memberId(),
            request.providerId(), request.procedureCode(), request.serviceDate(), request.amount()));
        return result.matchedClaimIds().stream().findFirst()
            .map(id -> new DuplicateMatch(id, result.classification().name(), 100,
                request.serviceDate().toString(), request.amount().toPlainString()))
            .orElse(null);
    }
}
