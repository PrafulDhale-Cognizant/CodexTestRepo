package com.flagship.claimcheck.model;

/** Stable machine-readable codes; messages may change without breaking consumers. */
public final class ReasonCodes {
    public static final String DUPLICATE_FOUND = "DUP-001";
    public static final String NO_DUPLICATE_FOUND = "DUP-000";
    public static final String HIGH_VALUE_MANUAL_REVIEW = "AMT-101";
    public static final String AMOUNT_WITHIN_AUTO_LIMIT = "AMT-000";
    public static final String BASIC_ELIGIBILITY_PASSED = "ELG-000";
    private ReasonCodes() {}
}
