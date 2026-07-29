package com.flagship.claimcheck.eligibility.rules;

import com.flagship.claimcheck.eligibility.*;

import java.util.Set;

public final class ProcedureRestrictionRule implements EligibilityRule {
    @Override public String name() { return "procedure-restriction"; }

    @Override public EligibilityRuleResult evaluate(EligibilityContext context) {
        if (context == null || context.member() == null || context.member().supportedProcedureCodes() == null) {
            return EligibilityRuleResult.passed(name(), "ELG-PRC-002", "No legacy procedure restriction is configured.");
        }
        Set<String> supported = context.member().supportedProcedureCodes();
        String procedure = context.claim() == null ? null : context.claim().procedureCode();
        if (procedure == null || procedure.isBlank()) {
            return EligibilityRuleResult.failed(name(), "ELG-PRC-003", "Procedure code is required.");
        }
        return supported.isEmpty() || supported.stream().anyMatch(procedure::equalsIgnoreCase)
            ? EligibilityRuleResult.passed(name(), "ELG-PRC-000", "Procedure is supported.")
            : EligibilityRuleResult.failed(name(), "ELG-PRC-001", "Procedure is not supported.");
    }
}
