package com.flagship.claimcheck.api;

import com.flagship.claimcheck.model.ClaimDecision;
import com.flagship.claimcheck.model.ClaimRequest;
import com.flagship.claimcheck.service.AdjudicationService;
import jakarta.validation.Valid;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {
    private final AdjudicationService service;
    private final ObservationRegistry observations;
    public ClaimController(AdjudicationService service, ObservationRegistry observations) {
        this.service = service;
        this.observations = observations;
    }

    @PostMapping("/adjudicate")
    @ResponseStatus(HttpStatus.OK)
    public ClaimDecision adjudicate(@Valid @RequestBody ClaimRequest claim,
                                    @RequestHeader(value = "X-Correlation-ID", required = false) String suppliedId) {
        String correlationId = suppliedId == null || !suppliedId.matches("[A-Za-z0-9._-]{1,128}")
            ? UUID.randomUUID().toString() : suppliedId;
        return Observation.createNotStarted("claim.controller", observations)
            .lowCardinalityKeyValue("operation", "adjudicate")
            .observe(() -> service.adjudicate(claim, correlationId));
    }
}
