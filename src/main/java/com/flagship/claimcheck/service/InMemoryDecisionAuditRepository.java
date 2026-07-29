package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.DecisionAuditRecord;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDecisionAuditRepository implements DecisionAuditRepository {
    private final Map<String, DecisionAuditRecord> decisions = new ConcurrentHashMap<>();

    @Override
    public DecisionAuditRecord save(DecisionAuditRecord decision) {
        decisions.put(decision.correlationId(), decision);
        return decision;
    }

    DecisionAuditRecord findByCorrelationId(String correlationId) {
        return decisions.get(correlationId);
    }
}
