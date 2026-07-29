package com.flagship.claimcheck.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The claim fields used by the legacy duplicate comparison. */
public record ClaimRecord(
    String claimId,
    String memberId,
    String providerId,
    String procedureCode,
    LocalDate serviceDate,
    BigDecimal amount
) {}
