package com.flagship.claimcheck.model;

import java.time.Instant;
import java.util.List;

public record ClaimDecision(
    String claimId, Status status, String headline, List<DecisionReason> reasons,
    DuplicateMatch duplicateMatch, String ruleSetVersion, String inputFingerprint,
    List<String> evaluatedRuleIds, List<String> matchingClaimReferences,
    DecisionTimestamps timestamps, long processingTimeMs, String correlationId, String traceId
) {
    public enum Status { APPROVED, REVIEW, DENIED }
    public record DecisionReason(String code, String message, Severity severity) {}
    public enum Severity { INFO, WARNING, ERROR }
    public record DuplicateMatch(String claimId, String matchType, int confidence, String serviceDate, String amount) {}
    public record DecisionTimestamps(Instant receivedAt, Instant evaluatedAt, Instant persistedAt) {}
}
