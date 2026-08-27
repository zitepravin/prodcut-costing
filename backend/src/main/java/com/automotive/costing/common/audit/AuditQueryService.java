package com.automotive.costing.common.audit;

import com.automotive.costing.common.audit.dto.AuditDtos.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Read-only access to the append-only audit_log table.
 * Intended for compliance, security, and administrative review (AUD-05).
 * Write path remains exclusively through {@link AuditService}.
 */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByUser(String userId, Instant from, Instant to, Pageable pageable) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();
        return auditLogRepository.findByUserIdAndTimestampBetween(userId, effectiveFrom, effectiveTo, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByEntityType(String entityType, Instant from, Instant to, Pageable pageable) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();
        return auditLogRepository.findByEntityTypeAndTimestampBetween(entityType, effectiveFrom, effectiveTo, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByEntityTypeAndAction(
            String entityType, String action, Instant from, Instant to, Pageable pageable) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();
        return auditLogRepository.findByEntityTypeAndActionAndTimestampBetween(
                        entityType, action, effectiveFrom, effectiveTo, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByAction(String action, Instant from, Instant to, Pageable pageable) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();
        return auditLogRepository.findByActionAndTimestampBetween(action, effectiveFrom, effectiveTo, pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLogEntry e) {
        return new AuditLogResponse(
                e.getAuditId(),
                e.getEntityType(),
                e.getEntityId(),
                e.getAction(),
                e.getOldValue(),
                e.getNewValue(),
                e.getUserId(),
                e.getTimestamp(),
                e.getIpAddress()
        );
    }
}
