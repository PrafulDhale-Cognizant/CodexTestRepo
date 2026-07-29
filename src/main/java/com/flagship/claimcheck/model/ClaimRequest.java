package com.flagship.claimcheck.model;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimRequest(
    @NotBlank @Pattern(regexp = "CLM-[0-9]{6}", message = "must match CLM-######") String claimId,
    @NotBlank @Pattern(regexp = "MBR-[0-9]{5}", message = "must match MBR-#####") String memberId,
    @NotBlank @Size(max = 20) String providerId,
    @NotBlank @Size(max = 10) String procedureCode,
    @NotBlank @Size(max = 10) String diagnosisCode,
    @NotBlank @Size(max = 3) String placeOfService,
    @NotNull @PastOrPresent LocalDate serviceDate,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal amount
) {}
