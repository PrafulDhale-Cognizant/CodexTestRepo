package com.flagship.claimcheck.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ReviewWorkItem(
    String workItemId, String claimId, String correlationId, String status,
    List<AdjudicationRecord.RuleResult> ruleResults, Map<String, String> evidence, Instant createdAt
) {}
