package com.flagship.claimcheck.service;

import com.flagship.claimcheck.api.IdempotencyKeyConflictException;
import com.flagship.claimcheck.model.ClaimRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** Atomically claims a canonical duplicate fingerprint. */
@Service
public class DuplicateClaimService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public DuplicateClaimService(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    public Registration register(ClaimRequest claim, String suppliedIdempotencyKey) {
        String fingerprint = fingerprint(claim);
        String idempotencyKey = normalizeKey(suppliedIdempotencyKey);

        if (idempotencyKey != null) {
            Registration replay = findByIdempotencyKey(idempotencyKey, fingerprint);
            if (replay != null) return replay;
        }

        try {
            transactions.executeWithoutResult(status -> jdbc.update("""
                insert into claim_registration (claim_id, fingerprint, idempotency_key)
                values (?, ?, ?)
                """, claim.claimId(), fingerprint, idempotencyKey));
            return new Registration(true, false, claim.claimId());
        } catch (DataIntegrityViolationException conflict) {
            // The failed insert transaction has completed before we inspect the winner.
            if (idempotencyKey != null) {
                Registration replay = findByIdempotencyKey(idempotencyKey, fingerprint);
                if (replay != null) return replay;
            }
            return jdbc.query("select claim_id from claim_registration where fingerprint = ?",
                    rs -> rs.next() ? new Registration(false, false, rs.getString(1)) : null, fingerprint);
        }
    }

    private Registration findByIdempotencyKey(String key, String fingerprint) {
        return jdbc.query("select claim_id, fingerprint from claim_registration where idempotency_key = ?", rs -> {
            if (!rs.next()) return null;
            if (!fingerprint.equals(rs.getString("fingerprint"))) throw new IdempotencyKeyConflictException();
            return new Registration(true, true, rs.getString("claim_id"));
        }, key);
    }

    /** Fields and normalization exactly match duplicate comparison semantics. */
    public String fingerprint(ClaimRequest claim) {
        String canonical = String.join("\u001f",
            claim.memberId(), claim.providerId().toUpperCase(Locale.ROOT), claim.procedureCode().toUpperCase(Locale.ROOT),
            claim.serviceDate().toString(), normalizeAmount(claim.amount()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the JVM", impossible);
        }
    }

    private static String normalizeAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) return null;
        String normalized = key.trim();
        if (normalized.length() > 200) throw new IllegalArgumentException("Idempotency-Key must not exceed 200 characters");
        return normalized;
    }

    public record Registration(boolean accepted, boolean replay, String registeredClaimId) {}
}
