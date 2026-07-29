package com.flagship.claimcheck.domain;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Repository
public class InMemoryClaimRepository implements ClaimRepository {
    private final Map<DuplicateKey, List<ClaimRecord>> duplicateIndex;

    public InMemoryClaimRepository() {
        this(List.of(
            new ClaimRecord("CLM-902184", "MBR-10482", "PRV-4481", "99213", LocalDate.of(2026, 7, 18), new BigDecimal("185.00")),
            new ClaimRecord("CLM-775091", "MBR-22019", "PRV-2204", "70553", LocalDate.of(2026, 7, 2), new BigDecimal("2400.00")),
            new ClaimRecord("CLM-881426", "MBR-10482", "PRV-4481", "80053", LocalDate.of(2026, 6, 21), new BigDecimal("96.40"))
        ));
    }

    public InMemoryClaimRepository(Collection<ClaimRecord> claims) {
        Map<DuplicateKey, List<ClaimRecord>> index = new HashMap<>();
        for (ClaimRecord claim : claims) {
            DuplicateKey.from(claim).ifPresent(key -> index.computeIfAbsent(key, ignored -> new ArrayList<>()).add(claim));
        }
        index.replaceAll((key, value) -> List.copyOf(value));
        duplicateIndex = Map.copyOf(index);
    }

    @Override
    public List<ClaimRecord> findAllByDuplicateKey(DuplicateKey key) {
        return duplicateIndex.getOrDefault(key, List.of());
    }
}
