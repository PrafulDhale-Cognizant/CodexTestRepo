package com.flagship.claimcheck.eligibility;

import com.flagship.claimcheck.model.ClaimRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityServiceTest {
    private final EligibilityService service = new EligibilityService();

    @Test void treatsEffectiveAndTerminationDatesAsInclusiveBoundaries() {
        LocalDate boundary = LocalDate.of(2026, 7, 18);
        var result = service.evaluate(context(claim(boundary, "99213", "MED"),
            new MemberEligibility(boundary, boundary, "ACTIVE", Set.of("MED"), Set.of("99213"))),
            AdjudicationPolicy.COLLECT_ALL_FAILURES);

        assertThat(result.eligible()).isTrue();
        assertThat(result.results()).hasSize(7).allMatch(EligibilityRuleResult::passed);
        assertThat(result.results()).extracting(EligibilityRuleResult::reasonCode)
            .contains("ELG-DATE-000", "ELG-BEN-000", "ELG-PRC-000");
    }

    @Test void rejectsDatesImmediatelyOutsideCoverageBoundaries() {
        LocalDate effective = LocalDate.of(2026, 7, 1);
        LocalDate termination = LocalDate.of(2026, 7, 31);

        var before = evaluate(claim(effective.minusDays(1), "99213", null),
            new MemberEligibility(effective, termination, "ACTIVE", Set.of(), Set.of()));
        var after = evaluate(claim(termination.plusDays(1), "99213", null),
            new MemberEligibility(effective, termination, "ACTIVE", Set.of(), Set.of()));

        assertThat(before.results()).extracting(EligibilityRuleResult::reasonCode).contains("ELG-DATE-001");
        assertThat(after.results()).extracting(EligibilityRuleResult::reasonCode).contains("ELG-DATE-002");
    }

    @Test void reportsMissingLegacyValuesWithoutThrowing() {
        var result = evaluate(claim(LocalDate.of(2026, 7, 18), "99213", null),
            new MemberEligibility(null, null, null, null, null));

        assertThat(result.eligible()).isFalse();
        assertThat(result.results()).extracting(EligibilityRuleResult::reasonCode)
            .contains("ELG-LEG-001", "ELG-DATE-003", "ELG-STS-002");
    }

    @Test void collectsAllFailuresWhenPolicyRequestsAnAuditTrail() {
        var claim = new ClaimRequest(null, "MBR-10482", null, "00000", LocalDate.of(2026, 6, 1), null, "DENTAL");
        var member = new MemberEligibility(LocalDate.of(2026, 7, 1), null, "SUSPENDED",
            Set.of("MED"), Set.of("99213"));

        var result = service.evaluate(context(claim, member), AdjudicationPolicy.COLLECT_ALL_FAILURES);

        assertThat(result.results()).hasSize(7);
        assertThat(result.results().stream().filter(r -> !r.passed()))
            .extracting(EligibilityRuleResult::reasonCode)
            .containsExactly("CLM-REQ-001", "ELG-DATE-001", "ELG-STS-001", "ELG-BEN-001", "ELG-PRC-001");
    }

    @Test void stopsAfterFirstFailureWhenPolicyRequestsShortCircuiting() {
        var invalidClaim = new ClaimRequest(null, null, null, null, null, null, null);

        var result = service.evaluate(context(invalidClaim, null), AdjudicationPolicy.STOP_ON_FIRST_FAILURE);

        assertThat(result.eligible()).isFalse();
        assertThat(result.results()).singleElement().satisfies(rule -> {
            assertThat(rule.rule()).isEqualTo("required-claim-fields");
            assertThat(rule.reasonCode()).isEqualTo("CLM-REQ-001");
        });
    }

    @Test void appliesBenefitAndProcedureRestrictionsCaseInsensitively() {
        var member = new MemberEligibility(LocalDate.of(2026, 1, 1), null, "active",
            Set.of("MED"), Set.of("99213"));

        assertThat(evaluate(claim(LocalDate.of(2026, 7, 18), "99213", "med"), member).eligible()).isTrue();
        assertThat(evaluate(claim(LocalDate.of(2026, 7, 18), "70553", "VISION"), member).results())
            .extracting(EligibilityRuleResult::reasonCode).contains("ELG-BEN-001", "ELG-PRC-001");
    }

    private EligibilityEvaluation evaluate(ClaimRequest claim, MemberEligibility member) {
        return service.evaluate(context(claim, member), AdjudicationPolicy.COLLECT_ALL_FAILURES);
    }

    private EligibilityContext context(ClaimRequest claim, MemberEligibility member) {
        return new EligibilityContext(claim, member);
    }

    private ClaimRequest claim(LocalDate date, String procedure, String benefit) {
        return new ClaimRequest("CLM-111111", "MBR-10482", "PRV-4481", procedure, date,
            new BigDecimal("185.00"), benefit);
    }
}
