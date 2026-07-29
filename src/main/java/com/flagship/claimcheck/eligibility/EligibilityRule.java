package com.flagship.claimcheck.eligibility;

public interface EligibilityRule {
    String name();

    EligibilityRuleResult evaluate(EligibilityContext context);
}
