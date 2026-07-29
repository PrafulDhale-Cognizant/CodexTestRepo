package com.flagship.claimcheck.eligibility.rules;

import com.flagship.claimcheck.eligibility.*;

import java.time.LocalDate;

public final class CoverageEffectiveDateRule implements EligibilityRule {
    @Override public String name() { return "coverage-effective-date"; }

    @Override public EligibilityRuleResult evaluate(EligibilityContext context) {
        LocalDate service = context == null || context.claim() == null ? null : context.claim().serviceDate();
        LocalDate effective = context == null || context.member() == null ? null : context.member().coverageEffectiveDate();
        if (service == null || effective == null) {
            return EligibilityRuleResult.failed(name(), "ELG-DATE-003", "Coverage effective date comparison cannot be performed.");
        }
        return service.isBefore(effective)
            ? EligibilityRuleResult.failed(name(), "ELG-DATE-001", "Service date is before coverage became effective.")
            : EligibilityRuleResult.passed(name(), "ELG-DATE-000", "Coverage was effective on the service date.");
    }
}
