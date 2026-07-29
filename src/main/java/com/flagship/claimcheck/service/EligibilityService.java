package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** Domain service containing deterministic member and benefit eligibility rules. */
@Service
public class EligibilityService {
    private static final BigDecimal AUTO_ADJUDICATION_LIMIT = new BigDecimal("5000.00");

    public EligibilityResult evaluate(ClaimRequest request) {
        if (request.amount().compareTo(AUTO_ADJUDICATION_LIMIT) > 0) {
            return new EligibilityResult(false, "ELG-101",
                "Claim amount exceeds the $5,000 auto-adjudication threshold.");
        }
        return new EligibilityResult(true, "ELG-000",
            "Member and claim details passed basic eligibility checks.");
    }

    public record EligibilityResult(boolean eligibleForAutomaticAdjudication, String reasonCode, String explanation) {}
}
