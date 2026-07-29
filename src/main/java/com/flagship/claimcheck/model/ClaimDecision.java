package com.flagship.claimcheck.model;

import java.time.Instant;
import java.util.List;

public record ClaimDecision(
    String claimId, Status status, String headline, List<DecisionReason> reasons,
    DuplicateMatch duplicateMatch, Instant adjudicatedAt, long processingTimeMs, String correlationId
) {
    public enum Status { APPROVED, PENDED, DENIED }
    public record DecisionReason(String code, String message, Severity severity) {}
    public enum Severity { INFO, WARNING, ERROR }
    public record DuplicateMatch(String claimId, String matchType, int confidence, String serviceDate, String amount) {}
}
