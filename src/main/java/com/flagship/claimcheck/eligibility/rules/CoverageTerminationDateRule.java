package com.flagship.claimcheck.eligibility.rules;

import com.flagship.claimcheck.eligibility.*;

import java.time.LocalDate;

public final class CoverageTerminationDateRule implements EligibilityRule {
    @Override public String name() { return "coverage-termination-date"; }

    @Override public EligibilityRuleResult evaluate(EligibilityContext context) {
        LocalDate service = context == null || context.claim() == null ? null : context.claim().serviceDate();
        if (service == null || context == null || context.member() == null) {
            return EligibilityRuleResult.failed(name(), "ELG-DATE-003", "Coverage termination date comparison cannot be performed.");
        }
        LocalDate termination = context.member().coverageTerminationDate();
        if (termination == null) {
            return EligibilityRuleResult.passed(name(), "ELG-DATE-004", "Coverage has no termination date.");
        }
        return service.isAfter(termination)
            ? EligibilityRuleResult.failed(name(), "ELG-DATE-002", "Service date is after coverage terminated.")
            : EligibilityRuleResult.passed(name(), "ELG-DATE-000", "Coverage had not terminated on the service date.");
    }
}
