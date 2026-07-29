package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision;
import com.flagship.claimcheck.model.ClaimRequest;
import com.flagship.claimcheck.shadow.ShadowExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AdjudicationService {
    private final LegacyCobolAdjudicator cobolAdjudicator;
    private final ShadowExecutionService shadowExecution;

    @Autowired
    public AdjudicationService(LegacyCobolAdjudicator cobolAdjudicator, ShadowExecutionService shadowExecution) {
        this.cobolAdjudicator = cobolAdjudicator;
        this.shadowExecution = shadowExecution;
    }

    /** Convenience constructor retained for small unit tests outside the Spring context. */
    public AdjudicationService() {
        this(new LegacyCobolAdjudicator(), null);
    }

    public ClaimDecision adjudicate(ClaimRequest request) {
        long start = System.nanoTime();
        String correlationId = UUID.randomUUID().toString();
        AdjudicationResult authoritative = cobolAdjudicator.evaluate(request);

        // Shadow failures cannot alter or prevent delivery of the authoritative result.
        if (shadowExecution != null) {
            shadowExecution.evaluate(correlationId, request, authoritative);
        }

        long elapsed = Math.max(1, (System.nanoTime() - start) / 1_000_000);
        return new ClaimDecision(request.claimId(), authoritative.status(), authoritative.headline(),
            authoritative.reasons(), authoritative.duplicateMatch(), Instant.now(), elapsed, correlationId);
    }
}
