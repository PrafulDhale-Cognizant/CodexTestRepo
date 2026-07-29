package com.flagship.claimcheck.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/** Normalized form of the five-field key used by the legacy comparison. */
public record DuplicateKey(
    String memberId,
    String providerId,
    String procedureCode,
    LocalDate serviceDate,
    BigDecimal amount
) {
    public static Optional<DuplicateKey> from(ClaimRecord claim) {
        if (claim == null || claim.serviceDate() == null || claim.amount() == null) {
            return Optional.empty();
        }
        String member = normalizeText(claim.memberId());
        String provider = normalizeText(claim.providerId());
        String procedure = normalizeText(claim.procedureCode());
        if (member == null || provider == null || procedure == null) {
            return Optional.empty();
        }
        return Optional.of(new DuplicateKey(member, provider, procedure, claim.serviceDate(),
            claim.amount().stripTrailingZeros()));
    }

    static String normalizeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
