package com.automotive.costing.common.event;

import java.time.Instant;

/**
 * Published when scorecard/TCO is refused due to BLOCKED/SUSPENDED lifecycle.
 * Must never auto-reinstate or approve suppliers.
 */
public record SupplierLifecycleOperationBlockedEvent(
        String blockId,
        String supplierId,
        String operationCode,
        String lifecycleStatus,
        String blockReason,
        Instant attemptedAt,
        String attemptedBy
) {}
