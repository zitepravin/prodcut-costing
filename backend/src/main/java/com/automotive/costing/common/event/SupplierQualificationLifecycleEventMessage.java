package com.automotive.costing.common.event;

import java.time.Instant;

/**
 * Spring application event for supplier qualification lifecycle changes.
 * Listeners may notify or integrate; must never auto-approve suppliers.
 */
public record SupplierQualificationLifecycleEventMessage(
        String eventId,
        String qualificationId,
        String supplierId,
        String eventType,
        String previousStatus,
        String newStatus,
        String recommendationId,
        Instant occurredAt,
        String occurredBy
) {}
