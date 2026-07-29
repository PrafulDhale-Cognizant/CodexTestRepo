package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimRequest;
import org.springframework.stereotype.Component;

/** Adapter for the authoritative COBOL result. Replace this prototype body with the host/copybook client. */
@Component
public class LegacyCobolAdjudicator implements ClaimAdjudicator {
    private final ClaimRules rules = new ClaimRules();

    @Override
    public AdjudicationResult evaluate(ClaimRequest request) {
        return rules.evaluate(request);
    }
}
