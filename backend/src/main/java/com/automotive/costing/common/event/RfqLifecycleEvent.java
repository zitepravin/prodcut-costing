package com.automotive.costing.common.event;

/**
 * Wave E — RFQ / quote lifecycle signal for notification listeners.
 * kind: CREATED | SENT | QUOTE_RECEIVED | QUOTE_ACCEPTED | QUOTE_REJECTED
 */
public record RfqLifecycleEvent(
        String kind,
        String rfqId,
        String quoteId,
        String recipientUserId,
        String summary
) {}
