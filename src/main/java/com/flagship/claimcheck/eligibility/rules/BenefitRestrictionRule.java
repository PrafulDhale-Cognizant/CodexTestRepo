package com.flagship.claimcheck.eligibility.rules;

import com.flagship.claimcheck.eligibility.*;

import java.util.Set;

public final class BenefitRestrictionRule implements EligibilityRule {
    @Override public String name() { return "benefit-restriction"; }

    @Override public EligibilityRuleResult evaluate(EligibilityContext context) {
        if (context == null || context.member() == null || context.member().supportedBenefitCodes() == null) {
            return EligibilityRuleResult.passed(name(), "ELG-BEN-002", "No legacy benefit restriction is configured.");
        }
        Set<String> supported = context.member().supportedBenefitCodes();
        String benefit = context.claim() == null ? null : context.claim().benefitCode();
        if (benefit == null || benefit.isBlank()) {
            return supported.isEmpty()
                ? EligibilityRuleResult.passed(name(), "ELG-BEN-002", "No benefit restriction applies.")
                : EligibilityRuleResult.failed(name(), "ELG-BEN-003", "Benefit code is required for this coverage.");
        }
        return supported.stream().anyMatch(benefit::equalsIgnoreCase)
            ? EligibilityRuleResult.passed(name(), "ELG-BEN-000", "Benefit is covered.")
            : EligibilityRuleResult.failed(name(), "ELG-BEN-001", "Benefit is not covered.");
    }
}
