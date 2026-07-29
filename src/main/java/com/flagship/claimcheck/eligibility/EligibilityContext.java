package com.flagship.claimcheck.eligibility;

import com.flagship.claimcheck.model.ClaimRequest;

public record EligibilityContext(ClaimRequest claim, MemberEligibility member) {}
