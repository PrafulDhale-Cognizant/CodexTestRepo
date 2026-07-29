package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimRequest;
import org.springframework.stereotype.Component;

/** Candidate Java implementation. Its result is observational while shadow mode is enabled. */
@Component
public class JavaAdjudicator implements ClaimAdjudicator {
    private final ClaimRules rules = new ClaimRules();

    @Override
    public AdjudicationResult evaluate(ClaimRequest request) {
        return rules.evaluate(request);
    }
}
