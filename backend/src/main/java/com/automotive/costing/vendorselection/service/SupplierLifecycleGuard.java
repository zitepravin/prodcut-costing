package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.event.SupplierLifecycleOperationBlockedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleOperationBlock;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.automotive.costing.vendorselection.repository.SupplierLifecycleOperationBlockRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Central hard-stop for operations that must not run on BLOCKED/SUSPENDED suppliers.
 * Aligns with eligibility, ranking, and comparison lifecycle gates.
 * Does not auto-reinstate or approve suppliers.
 */
@Service
@RequiredArgsConstructor
public class SupplierLifecycleGuard {

    public static final String OP_SCORECARD_EVALUATE = "SCORECARD_EVALUATE";
    public static final String OP_TCO_CALCULATE = "TCO_CALCULATE";

    private final SupplierQualificationCurrentRepository qualificationCurrentRepository;
    private final SupplierLifecycleOperationBlockRepository blockRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;

    public record BlockRecord(
            String blockId,
            String supplierId,
            String operationCode,
            String lifecycleStatus,
            String blockReason,
            Instant attemptedAt
    ) {}

    /**
     * Throws VS-LC-20 when lifecycle is BLOCKED or SUSPENDED.
     * Records an audit block row for traceability.
     */
    @Transactional
    public void assertNotBlockedOrSuspended(String supplierId, String operationCode) {
        if (supplierId == null || supplierId.isBlank()) {
            return;
        }
        String status = qualificationCurrentRepository.findById(supplierId)
                .map(SupplierQualificationCurrent::getLifecycleStatus)
                .map(Enum::name)
                .orElse(null);
        if (status == null) {
            return; // not initialized — other gates apply
        }
        if (!"BLOCKED".equalsIgnoreCase(status) && !"SUSPENDED".equalsIgnoreCase(status)) {
            return;
        }
        String reason = "Operation " + operationCode + " is not allowed while supplier lifecycle is "
                + status + ". Reinstate supplier before scoring or TCO for award decisions.";
        SupplierLifecycleOperationBlock block = new SupplierLifecycleOperationBlock();
        block.setBlockId(UUID.randomUUID().toString());
        block.setSupplierId(supplierId);
        block.setOperationCode(operationCode);
        block.setLifecycleStatus(status.toUpperCase());
        block.setBlockReason(reason);
        block.setAttemptedAt(Instant.now());
        block.setAttemptedBy(currentUserId());
        blockRepository.save(block);
        auditService.recordCreate("SupplierLifecycleOperationBlock", block.getBlockId(),
                operationCode + ":" + status);
        eventPublisher.publishEvent(new SupplierLifecycleOperationBlockedEvent(
                block.getBlockId(),
                supplierId,
                operationCode,
                status.toUpperCase(),
                reason,
                block.getAttemptedAt(),
                block.getAttemptedBy()));
        throw new BusinessRuleViolationException(reason, "VS-LC-20");
    }

    @Transactional(readOnly = true)
    public List<BlockRecord> listBlocks(String supplierId) {
        return blockRepository.findBySupplierIdOrderByAttemptedAtDesc(supplierId).stream()
                .map(b -> new BlockRecord(
                        b.getBlockId(),
                        b.getSupplierId(),
                        b.getOperationCode(),
                        b.getLifecycleStatus(),
                        b.getBlockReason(),
                        b.getAttemptedAt()))
                .toList();
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
