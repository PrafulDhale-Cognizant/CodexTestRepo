package com.flagship.claimcheck.eligibility;

import java.util.List;

public record EligibilityEvaluation(boolean eligible, List<EligibilityRuleResult> results) {
    public EligibilityEvaluation {
        results = List.copyOf(results);
    }
}
