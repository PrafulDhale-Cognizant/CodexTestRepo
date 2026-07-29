package com.flagship.claimcheck.shadow;

import org.springframework.stereotype.Component;

/** The sole promotion policy: all conditions must hold before Java may become authoritative. */
@Component
public class JavaDecisionReadinessGate {
    private final ShadowProperties properties;

    public JavaDecisionReadinessGate(ShadowProperties properties) {
        this.properties = properties;
    }

    public boolean isReady(ParityEvidence evidence) {
        ShadowProperties.Parity threshold = properties.parity();
        return evidence.agreementPercent() >= threshold.minimumAgreementPercent()
            && evidence.evaluationsPerWindow() >= threshold.minimumEvaluationsPerWindow()
            && evidence.consecutivePassingWindows() >= threshold.requiredConsecutiveWindows()
            && (!threshold.requireRepresentativeTraffic() || evidence.representativeTraffic());
    }
}
