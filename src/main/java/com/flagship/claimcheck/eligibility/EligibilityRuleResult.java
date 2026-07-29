package com.flagship.claimcheck.eligibility;

public record EligibilityRuleResult(
    String rule,
    RuleOutcome outcome,
    String reasonCode,
    String message
) {
    public static EligibilityRuleResult passed(String rule, String reasonCode, String message) {
        return new EligibilityRuleResult(rule, RuleOutcome.PASSED, reasonCode, message);
    }

    public static EligibilityRuleResult failed(String rule, String reasonCode, String message) {
        return new EligibilityRuleResult(rule, RuleOutcome.FAILED, reasonCode, message);
    }

    public boolean passed() {
        return outcome == RuleOutcome.PASSED;
    }
}
