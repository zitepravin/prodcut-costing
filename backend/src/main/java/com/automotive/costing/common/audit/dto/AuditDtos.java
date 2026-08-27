package com.automotive.costing.common.audit.dto;

import java.time.Instant;

/**
 * DTOs for audit log query API (SRS Module 19 / AUD-*).
 * Never expose the entity directly.
 */
public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditLogResponse(
            String auditId,
            String entityType,
            String entityId,
            String action,
            String oldValue,
            String newValue,
            String userId,
            Instant timestamp,
            String ipAddress
    ) {
    }

    public record AuditSearchCriteria(
            String entityType,
            String entityId,
            String userId,
            Instant from,
            Instant to
    ) {
    }
}
