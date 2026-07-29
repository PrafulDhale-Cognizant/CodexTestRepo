package com.flagship.claimcheck.model;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimRequest(
    @NotBlank @Pattern(regexp = "CLM-[0-9]{6}", message = "must match CLM-######") String claimId,
    @NotBlank @Pattern(regexp = "MBR-[0-9]{5}", message = "must match MBR-#####") String memberId,
    @NotBlank String providerId,
    @NotBlank String procedureCode,
    @NotNull @PastOrPresent LocalDate serviceDate,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal amount,
    String benefitCode
) {
    public ClaimRequest(String claimId, String memberId, String providerId, String procedureCode,
                        LocalDate serviceDate, BigDecimal amount) {
        this(claimId, memberId, providerId, procedureCode, serviceDate, amount, null);
    }
}
