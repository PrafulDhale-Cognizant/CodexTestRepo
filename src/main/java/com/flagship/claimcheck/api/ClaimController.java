package com.flagship.claimcheck.api;

import com.flagship.claimcheck.model.ClaimDecision;
import com.flagship.claimcheck.model.ClaimRequest;
import com.flagship.claimcheck.service.AdjudicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {
    private final AdjudicationService service;
    public ClaimController(AdjudicationService service) { this.service = service; }

    @PostMapping("/adjudicate")
    @ResponseStatus(HttpStatus.OK)
    public ClaimDecision adjudicate(@Valid @RequestBody ClaimRequest claim,
                                    @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return service.adjudicate(claim, correlationId);
    }
}
