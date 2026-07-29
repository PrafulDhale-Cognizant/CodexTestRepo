package com.flagship.claimcheck.shadow;

import com.flagship.claimcheck.model.ClaimDecision.*;
import com.flagship.claimcheck.model.ClaimRequest;
import com.flagship.claimcheck.service.AdjudicationResult;
import com.flagship.claimcheck.service.JavaAdjudicator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShadowExecutionServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-29T12:00:00Z");
    private final CapturingRecorder recorder = new CapturingRecorder();
    private final ShadowProperties properties = new ShadowProperties(true, "rules-v7",
        new ShadowProperties.Parity(99.5, 10_000, 14, true));

    @Test void recordsOnlySanitizedComparisonFields() {
        var service = service(new JavaAdjudicator());
        service.evaluate("correlation-123", claim(), approved());

        assertThat(recorder.comparison.claimCorrelationId()).isEqualTo("correlation-123");
        assertThat(recorder.comparison.cobolResult().disposition()).isEqualTo("APPROVED");
        assertThat(recorder.comparison.cobolResult().reasonCodes()).containsExactly("ELG-000");
        assertThat(recorder.comparison.ruleVersion()).isEqualTo("rules-v7");
        assertThat(recorder.comparison.evaluatedAt()).isEqualTo(NOW);
        assertThat(recorder.comparison.toString())
            .doesNotContain("MBR-12345", "PRV-sensitive", "185.00", "2026-07-18");
    }

    @Test void classifiesJavaFailureAsDefectWithoutThrowing() {
        JavaAdjudicator failing = new JavaAdjudicator() {
            @Override public AdjudicationResult evaluate(ClaimRequest request) {
                throw new IllegalStateException("sensitive member adapter payload");
            }
        };
        service(failing).evaluate("correlation-456", claim(), approved());

        assertThat(recorder.comparison.mismatchCategory()).isEqualTo(MismatchCategory.DEFECT);
        assertThat(recorder.comparison.javaResult().disposition()).isEqualTo("EVALUATION_ERROR");
        assertThat(recorder.comparison.toString()).doesNotContain("sensitive member adapter payload");
    }

    private ShadowExecutionService service(JavaAdjudicator javaAdjudicator) {
        return new ShadowExecutionService(javaAdjudicator, recorder, properties,
            Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ClaimRequest claim() {
        return new ClaimRequest("CLM-123456", "MBR-12345", "PRV-sensitive", "99214",
            LocalDate.parse("2026-07-18"), new BigDecimal("185.00"));
    }

    private static AdjudicationResult approved() {
        return new AdjudicationResult(Status.APPROVED, "approved",
            List.of(new DecisionReason("ELG-000", "sensitive message", Severity.INFO)), null);
    }

    private static final class CapturingRecorder extends ShadowComparisonRecorder {
        private ShadowComparison comparison;
        @Override public void record(ShadowComparison comparison) { this.comparison = comparison; }
    }
}
