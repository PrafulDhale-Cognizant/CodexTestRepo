package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimRequest;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class ReviewWorkItemServiceTest {
    @Test void repeatedRequestsReuseTheSameWorkItemAndSafeEvidence() {
        var repository = new AdjudicationRepository();
        var adjudication = new AdjudicationService(repository);
        var reviews = new ReviewWorkItemService(repository);
        var claim = new ClaimRequest("CLM-111114", "MBR-99001", "PRV-1000", "99214",
            LocalDate.parse("2026-07-18"), new BigDecimal("5000.01"));

        adjudication.adjudicate(claim, "corr-123");
        var first = reviews.createOrGet(claim.claimId());
        var repeated = reviews.createOrGet(claim.claimId());

        assertThat(repeated.workItemId()).isEqualTo(first.workItemId());
        assertThat(first.correlationId()).isEqualTo("corr-123");
        assertThat(first.ruleResults()).anyMatch(result -> result.reasonCode().equals("AMT-101"));
        assertThat(first.evidence()).containsOnlyKeys("procedureCode", "serviceDate", "amount");
    }
}
