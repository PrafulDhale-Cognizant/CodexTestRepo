package com.flagship.claimcheck.eligibility;

import com.flagship.claimcheck.eligibility.rules.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EligibilityService {
    private final List<EligibilityRule> rules;

    public EligibilityService() {
        this(List.of(
            new RequiredClaimFieldsRule(),
            new LegacyCoverageValuesRule(),
            new CoverageEffectiveDateRule(),
            new CoverageTerminationDateRule(),
            new MemberStatusRule(),
            new BenefitRestrictionRule(),
            new ProcedureRestrictionRule()
        ));
    }

    public EligibilityService(List<EligibilityRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public EligibilityEvaluation evaluate(EligibilityContext context, AdjudicationPolicy policy) {
        List<EligibilityRuleResult> results = new ArrayList<>();
        boolean eligible = true;
        for (EligibilityRule rule : rules) {
            EligibilityRuleResult result = rule.evaluate(context);
            results.add(result);
            if (!result.passed()) {
                eligible = false;
                if (policy == AdjudicationPolicy.STOP_ON_FIRST_FAILURE) {
                    break;
                }
            }
        }
        return new EligibilityEvaluation(eligible, results);
    }
}
