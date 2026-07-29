package com.flagship.claimcheck.domain;

import java.util.List;

public interface ClaimRepository {
    /** Performs an indexed lookup using all and only the fields in the exact duplicate key. */
    List<ClaimRecord> findAllByDuplicateKey(DuplicateKey key);
}
