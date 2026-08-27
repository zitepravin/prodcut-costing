package com.automotive.costing.common.event;

import java.time.Instant;

/**
 * Spring application event for recommendation human decisions.
 * Listeners may notify or integrate; must never auto-approve suppliers.
 */
public record VendorRecommendationDecisionEventMessage(
        String eventId,
        String recommendationId,
        String decisionId,
        String eventType,
        String previousStatus,
        String newStatus,
        String recommendedSupplierId,
        String overrideSupplierId,
        Instant occurredAt,
        String occurredBy
) {}
