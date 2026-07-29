package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.DecisionAuditRecord;

public interface DecisionAuditRepository {
    DecisionAuditRecord save(DecisionAuditRecord decision);
}
