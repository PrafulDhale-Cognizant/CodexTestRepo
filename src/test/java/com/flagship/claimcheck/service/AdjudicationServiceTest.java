package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision.Status;
import com.flagship.claimcheck.model.ClaimRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdjudicationServiceTest {
    @Autowired AdjudicationService service;

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

    private ClaimRequest claim(String id, String member, String provider, String procedure, String date, String amount) {
        return new ClaimRequest(id, member, provider, procedure, LocalDate.parse(date), new BigDecimal(amount));
    }
}
