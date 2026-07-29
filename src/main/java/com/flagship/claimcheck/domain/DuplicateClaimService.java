package com.flagship.claimcheck.domain;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DuplicateClaimService {
    public static final String EXACT_MATCH_REASON = "DUPLICATE_EXACT_MATCH";

    private final ClaimRepository repository;

    public DuplicateClaimService(ClaimRepository repository) {
        this.repository = repository;
    }

    public DuplicateClaimResult findDuplicates(ClaimRecord candidate) {
        var key = DuplicateKey.from(candidate);
        if (key.isEmpty()) {
            return DuplicateClaimResult.none();
        }
        String candidateId = DuplicateKey.normalizeText(candidate.claimId());
        List<String> matches = repository.findAllByDuplicateKey(key.get()).stream()
            .map(ClaimRecord::claimId)
            .filter(id -> !java.util.Objects.equals(DuplicateKey.normalizeText(id), candidateId))
            .sorted()
            .toList();
        return matches.isEmpty()
            ? DuplicateClaimResult.none()
            : new DuplicateClaimResult(matches, DuplicateClassification.EXACT, EXACT_MATCH_REASON);
    }

    public enum DuplicateClassification { NONE, EXACT }

    public record DuplicateClaimResult(
        List<String> matchedClaimIds,
        DuplicateClassification classification,
        String reasonCode
    ) {
        public DuplicateClaimResult {
            matchedClaimIds = List.copyOf(matchedClaimIds);
        }

        public static DuplicateClaimResult none() {
            return new DuplicateClaimResult(List.of(), DuplicateClassification.NONE, null);
        }
    }
}
