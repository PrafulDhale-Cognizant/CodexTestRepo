package com.flagship.claimcheck.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** The non-sensitive audit record retained for a decision. */
public record AdjudicationRecord(
    String claimId,
    ClaimDecision.Status decision,
    List<RuleResult> ruleResults,
    String correlationId,
    Map<String, String> evidence,
    Instant adjudicatedAt
) {
    public record RuleResult(String rule, Outcome outcome, String reasonCode) {}
    public enum Outcome { PASSED, FAILED, PENDED }
}
