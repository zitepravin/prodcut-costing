package com.automotive.costing.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit trail entry (SRS Module 19, AUD-01/AUD-05; TDD Section 4.7).
 *
 * Design intent: this entity has NO setters and the corresponding repository
 * (AuditLogRepository) exposes only save()/findBy* methods - no update or delete
 * operations exist anywhere in the codebase for this table. The database-level
 * enforcement (INSERT-only grant on the audit_log table for the application's
 * MySQL user, no UPDATE/DELETE grant) is the authoritative second line of
 * defense described in TDD Section 4.8, and must be configured in the DBA
 * runbook/migration for the production database user - not just in this class.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @Column(name = "audit_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String auditId;

    @Column(name = "entity_type", length = 50, nullable = false, updatable = false)
    private String entityType;

    @Column(name = "entity_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String entityId;

    @Column(name = "action", length = 20, nullable = false, updatable = false)
    private String action; // CREATE / UPDATE / DELETE / TRANSITION / VERIFY

    @Column(name = "old_value", columnDefinition = "json", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "json", updatable = false)
    private String newValue;

    @Column(name = "user_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    public static AuditLogEntry.AuditLogEntryBuilder newEntry() {
        return AuditLogEntry.builder()
                .auditId(UUID.randomUUID().toString())
                .timestamp(Instant.now());
    }
}
