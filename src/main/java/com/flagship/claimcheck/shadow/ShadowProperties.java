package com.flagship.claimcheck.shadow;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("claim-check.shadow")
public record ShadowProperties(boolean enabled, String ruleVersion, Parity parity) {
    public record Parity(double minimumAgreementPercent, int minimumEvaluationsPerWindow,
                         int requiredConsecutiveWindows, boolean requireRepresentativeTraffic) {}
}
