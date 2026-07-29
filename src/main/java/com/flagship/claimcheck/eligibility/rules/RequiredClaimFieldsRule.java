package com.flagship.claimcheck.eligibility.rules;

import com.flagship.claimcheck.eligibility.*;
import com.flagship.claimcheck.model.ClaimRequest;

import java.util.ArrayList;
import java.util.List;

public final class RequiredClaimFieldsRule implements EligibilityRule {
    @Override public String name() { return "required-claim-fields"; }

    @Override public EligibilityRuleResult evaluate(EligibilityContext context) {
        if (context == null || context.claim() == null) {
            return EligibilityRuleResult.failed(name(), "CLM-REQ-001", "Claim data is missing.");
        }
        ClaimRequest claim = context.claim();
        List<String> missing = new ArrayList<>();
        if (blank(claim.claimId())) missing.add("claimId");
        if (blank(claim.memberId())) missing.add("memberId");
        if (blank(claim.providerId())) missing.add("providerId");
        if (blank(claim.procedureCode())) missing.add("procedureCode");
        if (claim.serviceDate() == null) missing.add("serviceDate");
        if (claim.amount() == null) missing.add("amount");
        return missing.isEmpty()
            ? EligibilityRuleResult.passed(name(), "CLM-REQ-000", "All required claim fields are present.")
            : EligibilityRuleResult.failed(name(), "CLM-REQ-001", "Missing required claim fields: " + String.join(", ", missing) + ".");
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
