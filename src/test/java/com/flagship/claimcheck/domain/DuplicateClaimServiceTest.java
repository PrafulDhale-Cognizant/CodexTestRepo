package com.flagship.claimcheck.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateClaimServiceTest {
    private static final ClaimRecord STORED = claim("CLM-000001", "MBR-00001", "PRV-10", "a123",
        LocalDate.of(2026, 7, 1), "10.00");
    private final DuplicateClaimService service = serviceWith(STORED);

    @Test
    void returnsEveryExactMatchWithStableClassificationAndReason() {
        var second = claim("CLM-000002", "MBR-00001", "PRV-10", "A123", LocalDate.of(2026, 7, 1), "10");
        var result = serviceWith(STORED, second).findDuplicates(
            claim("CLM-999999", "MBR-00001", "PRV-10", "A123", LocalDate.of(2026, 7, 1), "10.0"));

        assertThat(result.matchedClaimIds()).containsExactly("CLM-000001", "CLM-000002");
        assertThat(result.classification()).isEqualTo(DuplicateClaimService.DuplicateClassification.EXACT);
        assertThat(result.reasonCode()).isEqualTo("DUPLICATE_EXACT_MATCH");
    }

    @Test
    void normalizesIdentifiersProcedureAndMonetaryScale() {
        var result = service.findDuplicates(
            claim(" clm-999999 ", " mbr-00001 ", " prv-10 ", " A123 ", LocalDate.of(2026, 7, 1), "10.000"));

        assertThat(result.matchedClaimIds()).containsExactly("CLM-000001");
    }

    @Test
    void comparesDatesByTheirLocalDateValue() {
        var result = service.findDuplicates(
            claim("CLM-999999", "MBR-00001", "PRV-10", "A123", LocalDate.parse("2026-07-01"), "10"));

        assertThat(result.matchedClaimIds()).containsExactly("CLM-000001");
    }

    @ParameterizedTest(name = "does not match when only {0} differs")
    @MethodSource("singleFieldDifferences")
    void doesNotMatchWhenOnlyOneKeyAttributeDiffers(String field, ClaimRecord candidate) {
        assertNoMatch(service.findDuplicates(candidate));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> singleFieldDifferences() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("memberId", claim("CLM-9", "MBR-00002", "PRV-10", "A123", LocalDate.of(2026, 7, 1), "10")),
            org.junit.jupiter.params.provider.Arguments.of("providerId", claim("CLM-9", "MBR-00001", "PRV-11", "A123", LocalDate.of(2026, 7, 1), "10")),
            org.junit.jupiter.params.provider.Arguments.of("procedureCode", claim("CLM-9", "MBR-00001", "PRV-10", "A124", LocalDate.of(2026, 7, 1), "10")),
            org.junit.jupiter.params.provider.Arguments.of("serviceDate", claim("CLM-9", "MBR-00001", "PRV-10", "A123", LocalDate.of(2026, 7, 2), "10")),
            org.junit.jupiter.params.provider.Arguments.of("amount", claim("CLM-9", "MBR-00001", "PRV-10", "A123", LocalDate.of(2026, 7, 1), "10.01"))
        );
    }

    @ParameterizedTest
    @MethodSource("claimsWithNullKeyFields")
    void nullKeyFieldsProduceNoMatch(ClaimRecord candidate) {
        assertNoMatch(service.findDuplicates(candidate));
    }

    static Stream<ClaimRecord> claimsWithNullKeyFields() {
        return Stream.of(
            claim("CLM-9", null, "PRV-10", "A123", LocalDate.of(2026, 7, 1), "10"),
            claim("CLM-9", "MBR-00001", null, "A123", LocalDate.of(2026, 7, 1), "10"),
            claim("CLM-9", "MBR-00001", "PRV-10", null, LocalDate.of(2026, 7, 1), "10"),
            claim("CLM-9", "MBR-00001", "PRV-10", "A123", null, "10"),
            claim("CLM-9", "MBR-00001", "PRV-10", "A123", LocalDate.of(2026, 7, 1), null)
        );
    }

    @Test
    void nullClaimProducesNoMatch() {
        assertNoMatch(service.findDuplicates(null));
    }

    @Test
    void excludesTheCandidateFromItsOwnMatchesAfterIdNormalization() {
        assertNoMatch(service.findDuplicates(
            claim(" clm-000001 ", "MBR-00001", "PRV-10", "A123", LocalDate.of(2026, 7, 1), "10")));
    }

    private static void assertNoMatch(DuplicateClaimService.DuplicateClaimResult result) {
        assertThat(result.matchedClaimIds()).isEmpty();
        assertThat(result.classification()).isEqualTo(DuplicateClaimService.DuplicateClassification.NONE);
        assertThat(result.reasonCode()).isNull();
    }

    private static DuplicateClaimService serviceWith(ClaimRecord... records) {
        return new DuplicateClaimService(new InMemoryClaimRepository(List.of(records)));
    }

    private static ClaimRecord claim(String id, String member, String provider, String procedure,
                                     LocalDate date, String amount) {
        return new ClaimRecord(id, member, provider, procedure, date, amount == null ? null : new BigDecimal(amount));
    }
}
