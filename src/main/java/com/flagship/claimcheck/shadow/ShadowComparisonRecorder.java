package com.flagship.claimcheck.shadow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Uses parameterized structured fields and only the allow-listed comparison DTO. */
@Component
public class ShadowComparisonRecorder {
    private static final Logger LOG = LoggerFactory.getLogger("claim-shadow-comparison");

    public void record(ShadowComparison comparison) {
        LOG.info("shadow_comparison correlation_id={} cobol_disposition={} cobol_reason_codes={} "
                + "java_disposition={} java_reason_codes={} cobol_only_reason_codes={} java_only_reason_codes={} "
                + "mismatch_category={} rule_version={} evaluated_at={}",
            comparison.claimCorrelationId(), comparison.cobolResult().disposition(), comparison.cobolResult().reasonCodes(),
            comparison.javaResult().disposition(), comparison.javaResult().reasonCodes(),
            comparison.reasonCodeDifferences().cobolOnly(), comparison.reasonCodeDifferences().javaOnly(),
            comparison.mismatchCategory(), comparison.ruleVersion(), comparison.evaluatedAt());
    }
}
