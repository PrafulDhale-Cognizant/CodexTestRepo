package com.flagship.claimcheck.eligibility.rules;

import com.flagship.claimcheck.eligibility.*;

public final class MemberStatusRule implements EligibilityRule {
    @Override public String name() { return "member-status"; }

    @Override public EligibilityRuleResult evaluate(EligibilityContext context) {
        String status = context == null || context.member() == null ? null : context.member().memberStatus();
        if (status == null || status.isBlank()) {
            return EligibilityRuleResult.failed(name(), "ELG-STS-002", "Member status is missing from the legacy record.");
        }
        return "ACTIVE".equalsIgnoreCase(status.trim())
            ? EligibilityRuleResult.passed(name(), "ELG-STS-000", "Member status is active.")
            : EligibilityRuleResult.failed(name(), "ELG-STS-001", "Member status is not active.");
    }
}
