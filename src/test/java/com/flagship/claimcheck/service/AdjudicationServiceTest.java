package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision.Status;
import com.flagship.claimcheck.model.ClaimRequest;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.Optional;

class AdjudicationServiceTest {
    private final AdjudicationService service = new AdjudicationService();

    @Test void deniesAnExactLegacyDuplicate() {
        var claim = claim("CLM-111111", "MBR-10482", "PRV-4481", "99213", "2026-07-18", "185.00");
        var result = service.adjudicate(claim);
        assertThat(result.status()).isEqualTo(Status.DENIED);
        assertThat(result.duplicateMatch().claimId()).isEqualTo("CLM-902184");
    }

    @Test void approvesAnEligibleUniqueClaim() {
        var result = service.adjudicate(claim("CLM-111112", "MBR-10482", "PRV-4481", "99214", "2026-07-18", "185.00"));
        assertThat(result.status()).isEqualTo(Status.APPROVED);
        assertThat(result.duplicateMatch()).isNull();
    }

    @Test void routesHighValueClaimForReview() {
        var result = service.adjudicate(claim("CLM-111113", "MBR-99001", "PRV-1000", "99214", "2026-07-18", "5000.01"));
        assertThat(result.status()).isEqualTo(Status.REVIEW);
        assertThat(result.reasons()).extracting("code").containsExactly("AMT-101");
    }

    @Test void returnsVersionedAuditableDecisionWithoutRawInputPersistence() {
        var repository = new CapturingRepository();
        var meters = new SimpleMeterRegistry();
        var auditedService = new AdjudicationService(repository, meters, ObservationRegistry.NOOP, Optional.empty());

        var result = auditedService.adjudicate(
            claim("CLM-111114", "MBR-99001", "PRV-1000", "99214", "2026-07-18", "80.00"), "corr-123");

        assertThat(result.ruleSetVersion()).isEqualTo(AdjudicationService.RULE_SET_VERSION);
        assertThat(result.inputFingerprint()).hasSize(64).doesNotContain("MBR-99001");
        assertThat(result.evaluatedRuleIds()).containsExactly(
            AdjudicationService.ELIGIBILITY_RULE, AdjudicationService.DUPLICATE_RULE, AdjudicationService.AMOUNT_RULE);
        assertThat(repository.saved.correlationId()).isEqualTo("corr-123");
        assertThat(repository.saved.inputFingerprint()).isEqualTo(result.inputFingerprint());
        assertThat(repository.saved.reasonCodes()).containsExactly("ELG-000", "DUP-000");
        assertThat(repository.saved.persistedAt()).isNotNull();
        assertThat(meters.counter("claim.decisions", "outcome", "APPROVED").count()).isEqualTo(1);
    }

    private static class CapturingRepository implements DecisionAuditRepository {
        private com.flagship.claimcheck.model.DecisionAuditRecord saved;
        @Override public com.flagship.claimcheck.model.DecisionAuditRecord save(
            com.flagship.claimcheck.model.DecisionAuditRecord decision) {
            saved = decision;
            return decision;
        }
    }

    private ClaimRequest claim(String id, String member, String provider, String procedure, String date, String amount) {
        return new ClaimRequest(id, member, provider, procedure, LocalDate.parse(date), new BigDecimal(amount));
    }
}
