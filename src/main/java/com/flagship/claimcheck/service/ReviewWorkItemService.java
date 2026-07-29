package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.AdjudicationRecord;
import com.flagship.claimcheck.model.ClaimDecision.Status;
import com.flagship.claimcheck.model.ReviewWorkItem;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReviewWorkItemService {
    private final AdjudicationRepository adjudications;
    private final ConcurrentHashMap<String, ReviewWorkItem> itemsByClaim = new ConcurrentHashMap<>();

    public ReviewWorkItemService(AdjudicationRepository adjudications) { this.adjudications = adjudications; }

    public ReviewWorkItem createOrGet(String claimId) {
        AdjudicationRecord record = adjudications.findByClaimId(claimId)
            .filter(saved -> saved.decision() == Status.PENDED)
            .orElseThrow(() -> new NoSuchElementException("No pended adjudication exists for claim " + claimId));
        return itemsByClaim.computeIfAbsent(claimId, ignored -> new ReviewWorkItem(
            "RWI-" + UUID.randomUUID(), claimId, record.correlationId(), "OPEN",
            record.ruleResults(), record.evidence(), Instant.now()));
    }
}
