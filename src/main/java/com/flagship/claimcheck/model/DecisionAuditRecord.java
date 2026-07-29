package com.flagship.claimcheck.model;

import java.time.Instant;
import java.util.List;

/** The deliberately PHI-minimized, immutable representation retained for audit. */
public record DecisionAuditRecord(
    String correlationId,
    String ruleSetVersion,
    String inputFingerprint,
    ClaimDecision.Status outcome,
    List<String> reasonCodes,
    List<String> evaluatedRuleIds,
    List<String> matchingClaimReferences,
    Instant receivedAt,
    Instant evaluatedAt,
    Instant persistedAt,
    String traceId
) {}
