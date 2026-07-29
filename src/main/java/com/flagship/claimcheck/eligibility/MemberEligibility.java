package com.flagship.claimcheck.eligibility;

import java.time.LocalDate;
import java.util.Set;

/** Values read from the legacy member/coverage record. Null represents an absent legacy value. */
public record MemberEligibility(
    LocalDate coverageEffectiveDate,
    LocalDate coverageTerminationDate,
    String memberStatus,
    Set<String> supportedBenefitCodes,
    Set<String> supportedProcedureCodes
) {
    public MemberEligibility {
        supportedBenefitCodes = immutableCopy(supportedBenefitCodes);
        supportedProcedureCodes = immutableCopy(supportedProcedureCodes);
    }

    private static Set<String> immutableCopy(Set<String> values) {
        return values == null ? null : Set.copyOf(values);
    }
}
