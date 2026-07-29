package com.flagship.claimcheck.shadow;

/** Aggregated, non-PHI evidence supplied by production observability. */
public record ParityEvidence(double agreementPercent, int evaluationsPerWindow,
                             int consecutivePassingWindows, boolean representativeTraffic) {
}
