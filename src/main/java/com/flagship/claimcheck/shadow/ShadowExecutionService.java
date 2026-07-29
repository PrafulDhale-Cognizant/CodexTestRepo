package com.flagship.claimcheck.shadow;

import com.flagship.claimcheck.model.ClaimDecision.DecisionReason;
import com.flagship.claimcheck.model.ClaimRequest;
import com.flagship.claimcheck.service.AdjudicationResult;
import com.flagship.claimcheck.service.JavaAdjudicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;

import static com.flagship.claimcheck.shadow.ShadowComparison.*;

@Service
public class ShadowExecutionService {
    private static final Logger LOG = LoggerFactory.getLogger(ShadowExecutionService.class);
    private final JavaAdjudicator javaAdjudicator;
    private final ShadowComparisonRecorder recorder;
    private final ShadowProperties properties;
    private final Clock clock;

    public ShadowExecutionService(JavaAdjudicator javaAdjudicator, ShadowComparisonRecorder recorder,
                                  ShadowProperties properties) {
        this(javaAdjudicator, recorder, properties, Clock.systemUTC());
    }

    ShadowExecutionService(JavaAdjudicator javaAdjudicator, ShadowComparisonRecorder recorder,
                           ShadowProperties properties, Clock clock) {
        this.javaAdjudicator = javaAdjudicator;
        this.recorder = recorder;
        this.properties = properties;
        this.clock = clock;
    }

    public void evaluate(String correlationId, ClaimRequest request, AdjudicationResult cobolResult) {
        if (!properties.enabled()) return;
        try {
            AdjudicationResult javaResult = javaAdjudicator.evaluate(request);
            recorder.record(compare(correlationId, cobolResult, javaResult, properties.ruleVersion()));
        } catch (RuntimeException exception) {
            // Never log the exception or request: either can contain sensitive adapter payloads.
            recorder.record(new ShadowComparison(correlationId, sanitized(cobolResult),
                new SanitizedResult("EVALUATION_ERROR", Set.of()),
                new ReasonCodeDifferences(reasonCodes(cobolResult), Set.of()),
                MismatchCategory.DEFECT, properties.ruleVersion(), clock.instant()));
            LOG.warn("Java shadow evaluation failed; authoritative COBOL disposition retained correlation_id={}", correlationId);
        }
    }

    private ShadowComparison compare(String correlationId, AdjudicationResult cobol, AdjudicationResult java,
                                     String ruleVersion) {
        Set<String> cobolCodes = reasonCodes(cobol);
        Set<String> javaCodes = reasonCodes(java);
        Set<String> cobolOnly = new HashSet<>(cobolCodes);
        cobolOnly.removeAll(javaCodes);
        Set<String> javaOnly = new HashSet<>(javaCodes);
        javaOnly.removeAll(cobolCodes);
        MismatchCategory category;
        if (cobol.status() == java.status() && cobolOnly.isEmpty() && javaOnly.isEmpty()) {
            category = MismatchCategory.NONE;
        } else if (cobol.status() == java.status()) {
            category = MismatchCategory.EXPECTED_REPRESENTATION_DIFFERENCE;
        } else if (containsTimingCode(cobolOnly) || containsTimingCode(javaOnly)) {
            category = MismatchCategory.DATA_TIMING_DIFFERENCE;
        } else {
            category = MismatchCategory.RULE_DIFFERENCE;
        }
        return new ShadowComparison(correlationId, sanitized(cobol), sanitized(java),
            new ReasonCodeDifferences(cobolOnly, javaOnly), category, ruleVersion, clock.instant());
    }

    private static boolean containsTimingCode(Set<String> codes) {
        return codes.stream().anyMatch(code -> code.startsWith("DATA-") || code.startsWith("TIMING-"));
    }

    private static SanitizedResult sanitized(AdjudicationResult result) {
        return new SanitizedResult(result.status().name(), reasonCodes(result));
    }

    private static Set<String> reasonCodes(AdjudicationResult result) {
        return result.reasons().stream().map(DecisionReason::code).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
