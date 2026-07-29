package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision.DecisionReason;
import com.flagship.claimcheck.model.ClaimDecision.DuplicateMatch;
import com.flagship.claimcheck.model.ClaimDecision.Status;

import java.util.List;

/** Internal result shared by adjudication adapters; it is never written to shadow logs. */
public record AdjudicationResult(Status status, String headline, List<DecisionReason> reasons,
                                 DuplicateMatch duplicateMatch) {
}
