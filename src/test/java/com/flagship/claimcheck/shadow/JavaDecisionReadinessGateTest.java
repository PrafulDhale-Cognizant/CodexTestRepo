package com.flagship.claimcheck.shadow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaDecisionReadinessGateTest {
    private final JavaDecisionReadinessGate gate = new JavaDecisionReadinessGate(
        new ShadowProperties(true, "v1", new ShadowProperties.Parity(99.5, 10_000, 14, true)));

    @Test void requiresThresholdVolumeDurationAndRepresentativeTraffic() {
        assertThat(gate.isReady(new ParityEvidence(99.5, 10_000, 14, true))).isTrue();
        assertThat(gate.isReady(new ParityEvidence(99.49, 10_000, 14, true))).isFalse();
        assertThat(gate.isReady(new ParityEvidence(99.5, 9_999, 14, true))).isFalse();
        assertThat(gate.isReady(new ParityEvidence(99.5, 10_000, 13, true))).isFalse();
        assertThat(gate.isReady(new ParityEvidence(99.5, 10_000, 14, false))).isFalse();
    }
}
