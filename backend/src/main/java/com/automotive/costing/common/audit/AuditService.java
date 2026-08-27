package com.automotive.costing.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The ONLY supported way to write to the audit_log table (MDM-07, AUD-01).
 * Every service that creates/updates/deletes a cost-affecting or master-data record
 * must call this after (or within the same transaction as) its own write, per
 * TDD Section 8.3: "All write operations to cost_record, master data tables, workflow,
 * and user/role tables must emit an audit_log entry within the same transaction."
 *
 * REQUIRES_NEW propagation is deliberately NOT used here - the audit write must
 * commit or roll back atomically with the business write it documents, so it
 * participates in the caller's existing transaction (default propagation).
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordCreate(String entityType, String entityId, Object newValue) {
        record(entityType, entityId, "CREATE", null, newValue);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordUpdate(String entityType, String entityId, Object oldValue, Object newValue) {
        record(entityType, entityId, "UPDATE", oldValue, newValue);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordDelete(String entityType, String entityId, Object oldValue) {
        record(entityType, entityId, "DELETE", oldValue, null);
    }

    /**
     * Status / lifecycle transition (V87). Distinct from generic UPDATE so compliance
     * queries can filter action=TRANSITION without scanning all updates.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordTransition(String entityType, String entityId, Object fromStatus, Object toStatus) {
        record(entityType, entityId, "TRANSITION", fromStatus, toStatus);
    }

    /**
     * Evidence verification outcome (V87). Separates VERIFY from commercial UPDATE/TRANSITION.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordVerify(String entityType, String entityId, Object previousEvidenceStatus, Object newEvidenceStatus) {
        record(entityType, entityId, "VERIFY", previousEvidenceStatus, newEvidenceStatus);
    }

    /**
     * SEC-05: login/logout/MFA events. Deliberately REQUIRES_NEW, not MANDATORY like the
     * CRUD-audit methods above - a failed-login audit entry must survive even though the
     * transaction it's called from (e.g. AuthService.login()) is about to roll back and
     * rethrow, since the failed attempt itself is exactly the thing worth recording. An
     * independent transaction commits this write regardless of the caller's outcome.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthEvent(String action, String userId) {
        AuditLogEntry entry = AuditLogEntry.newEntry()
                .entityType("AuthEvent")
                .entityId(userId)
                .action(action)
                .userId(userId)
                .ipAddress(currentIpAddress())
                .build();
        auditLogRepository.save(entry);
    }

    private void record(String entityType, String entityId, String action, Object oldValue, Object newValue) {
        AuditLogEntry entry = AuditLogEntry.newEntry()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(toJson(oldValue))
                .newValue(toJson(newValue))
                .userId(currentUserId())
                .ipAddress(currentIpAddress())
                .build();
        auditLogRepository.save(entry);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            // Never let audit-serialization failures block the underlying business transaction's
            // audit intent from being visible - fall back to toString() rather than throwing.
            return String.valueOf(value);
        }
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "system";
        }
        return authentication.getName();
    }

    private String currentIpAddress() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
