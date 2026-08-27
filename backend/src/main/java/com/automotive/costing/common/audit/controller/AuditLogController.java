package com.automotive.costing.common.audit.controller;

import com.automotive.costing.common.audit.AuditQueryService;
import com.automotive.costing.common.audit.dto.AuditDtos.AuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Compliance / security audit trail query API.
 * Restricted to administrative roles only (AUD-05).
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Log")
public class AuditLogController {

    private final AuditQueryService auditQueryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Query audit log entries",
            description = "Filter by entity (type+id), by user, or by entity type within a time window. "
                    + "Results are newest-first by default.")
    public ResponseEntity<Page<AuditLogResponse>> search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AuditLogResponse> page;
        String act = action != null && !action.isBlank() ? action.trim().toUpperCase() : null;
        if (entityType != null && entityId != null && !entityType.isBlank() && !entityId.isBlank()) {
            page = auditQueryService.findByEntity(entityType, entityId, pageable);
        } else if (userId != null && !userId.isBlank()) {
            page = auditQueryService.findByUser(userId, from, to, pageable);
        } else if (entityType != null && !entityType.isBlank() && act != null) {
            page = auditQueryService.findByEntityTypeAndAction(entityType, act, from, to, pageable);
        } else if (entityType != null && !entityType.isBlank()) {
            page = auditQueryService.findByEntityType(entityType, from, to, pageable);
        } else if (act != null) {
            page = auditQueryService.findByAction(act, from, to, pageable);
        } else {
            // Require at least one filter: entityType+entityId, userId, entityType, or action
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(page);
    }
}
