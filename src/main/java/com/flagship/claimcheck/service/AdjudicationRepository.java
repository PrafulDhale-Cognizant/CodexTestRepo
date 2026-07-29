package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.AdjudicationRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AdjudicationRepository {
    private final ConcurrentHashMap<String, AdjudicationRecord> records = new ConcurrentHashMap<>();

    public void save(AdjudicationRecord record) { records.put(record.claimId(), record); }
    public Optional<AdjudicationRecord> findByClaimId(String claimId) { return Optional.ofNullable(records.get(claimId)); }
}
