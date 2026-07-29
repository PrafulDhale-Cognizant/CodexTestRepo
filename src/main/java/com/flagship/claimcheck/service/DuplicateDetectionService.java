package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision.DuplicateMatch;
import com.flagship.claimcheck.model.ClaimRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Domain service for matching a claim against the legacy claim index. */
@Service
public class DuplicateDetectionService {
    private final List<LegacyClaim> legacyClaims = List.of(
        new LegacyClaim("CLM-902184", "MBR-10482", "PRV-4481", "99213", LocalDate.of(2026, 7, 18), new BigDecimal("185.00")),
        new LegacyClaim("CLM-775091", "MBR-22019", "PRV-2204", "70553", LocalDate.of(2026, 7, 2), new BigDecimal("2400.00")),
        new LegacyClaim("CLM-881426", "MBR-10482", "PRV-4481", "80053", LocalDate.of(2026, 6, 21), new BigDecimal("96.40"))
    );

    public Optional<DuplicateMatch> findExactDuplicate(ClaimRequest request) {
        return legacyClaims.stream()
            .filter(claim -> claim.memberId.equals(request.memberId())
                && claim.providerId.equalsIgnoreCase(request.providerId())
                && claim.procedureCode.equalsIgnoreCase(request.procedureCode())
                && claim.serviceDate.equals(request.serviceDate())
                && claim.amount.compareTo(request.amount()) == 0)
            .findFirst()
            .map(claim -> new DuplicateMatch(claim.claimId, "EXACT", 100,
                claim.serviceDate.toString(), claim.amount.toPlainString()));
    }

    private record LegacyClaim(String claimId, String memberId, String providerId, String procedureCode,
                               LocalDate serviceDate, BigDecimal amount) {}
}
