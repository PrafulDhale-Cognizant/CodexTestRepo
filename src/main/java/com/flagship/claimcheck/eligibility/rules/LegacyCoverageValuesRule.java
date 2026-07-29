package com.flagship.claimcheck.eligibility.rules;

import com.flagship.claimcheck.eligibility.*;

import java.util.ArrayList;
import java.util.List;

public final class LegacyCoverageValuesRule implements EligibilityRule {
    @Override public String name() { return "legacy-coverage-values"; }

    @Override public EligibilityRuleResult evaluate(EligibilityContext context) {
        if (context == null || context.member() == null) {
            return EligibilityRuleResult.failed(name(), "ELG-LEG-001", "Legacy eligibility record is missing.");
        }
        List<String> missing = new ArrayList<>();
        if (context.member().coverageEffectiveDate() == null) missing.add("coverageEffectiveDate");
        if (context.member().memberStatus() == null || context.member().memberStatus().isBlank()) missing.add("memberStatus");
        return missing.isEmpty()
            ? EligibilityRuleResult.passed(name(), "ELG-LEG-000", "Required legacy eligibility values are present.")
            : EligibilityRuleResult.failed(name(), "ELG-LEG-001", "Missing legacy values: " + String.join(", ", missing) + ".");
    }
}
