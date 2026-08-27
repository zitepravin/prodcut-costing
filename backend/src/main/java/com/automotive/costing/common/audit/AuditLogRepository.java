package com.automotive.costing.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Deliberately extends only CrudRepository (which does technically expose delete methods
 * at the interface level) but this repository is NEVER wired into any service that calls
 * them - AuditService below exposes save() and query methods only. The true enforcement
 * boundary is the MySQL grant (INSERT-only) on the audit_log table, per TDD Section 4.8.
 */
@Repository
public interface AuditLogRepository extends CrudRepository<AuditLogEntry, String> {

    Page<AuditLogEntry> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<AuditLogEntry> findByUserIdAndTimestampBetween(String userId, Instant from, Instant to, Pageable pageable);

    Page<AuditLogEntry> findByEntityTypeAndActionAndTimestampBetween(
            String entityType, String action, Instant from, Instant to, Pageable pageable);

    Page<AuditLogEntry> findByActionAndTimestampBetween(
            String action, Instant from, Instant to, Pageable pageable);

    Page<AuditLogEntry> findByEntityTypeAndTimestampBetween(String entityType, Instant from, Instant to,
                                                             Pageable pageable);
}
