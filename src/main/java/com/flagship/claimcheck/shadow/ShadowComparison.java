package com.flagship.claimcheck.shadow;

import java.time.Instant;
import java.util.Set;

/**
 * Deliberately allow-listed shadow telemetry. Do not add request data, claim IDs,
 * member IDs, provider data, dates of service, amounts, or reason messages.
 */
public record ShadowComparison(
    String claimCorrelationId,
    SanitizedResult cobolResult,
    SanitizedResult javaResult,
    ReasonCodeDifferences reasonCodeDifferences,
    MismatchCategory mismatchCategory,
    String ruleVersion,
    Instant evaluatedAt
) {
    public record SanitizedResult(String disposition, Set<String> reasonCodes) {
        public SanitizedResult {
            reasonCodes = Set.copyOf(reasonCodes);
        }
    }

    public record ReasonCodeDifferences(Set<String> cobolOnly, Set<String> javaOnly) {
        public ReasonCodeDifferences {
            cobolOnly = Set.copyOf(cobolOnly);
            javaOnly = Set.copyOf(javaOnly);
        }
    }
}
