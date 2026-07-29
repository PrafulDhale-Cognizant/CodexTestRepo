package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimRequest;

@FunctionalInterface
public interface ClaimAdjudicator {
    AdjudicationResult evaluate(ClaimRequest request);
}
