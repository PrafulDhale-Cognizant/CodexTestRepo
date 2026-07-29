package com.flagship.claimcheck.api;

import com.flagship.claimcheck.model.ReviewWorkItem;
import com.flagship.claimcheck.service.ReviewWorkItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/review-work-items")
public class ReviewController {
    private final ReviewWorkItemService service;
    public ReviewController(ReviewWorkItemService service) { this.service = service; }

    @PostMapping
    public ReviewWorkItem createOrGet(@Valid @RequestBody CreateReviewRequest request) {
        return service.createOrGet(request.claimId());
    }

    public record CreateReviewRequest(
        @NotBlank @Pattern(regexp = "CLM-[0-9]{6}", message = "must match CLM-######") String claimId
    ) {}
}
