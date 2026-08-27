package com.automotive.costing.notification.listener;

import com.automotive.costing.common.event.RfqLifecycleEvent;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.service.NotificationEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Wave E — RFQ / quote events → in-app notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RfqNotificationListener {

    private final NotificationEventService notificationEventService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRfqLifecycle(RfqLifecycleEvent event) {
        if (event == null || event.recipientUserId() == null || event.recipientUserId().isBlank()) {
            return;
        }
        NotificationType type = mapType(event.kind());
        String title = switch (type) {
            case RFQ_CREATED -> "RFQ created";
            case RFQ_SENT -> "RFQ sent";
            case QUOTE_RECEIVED -> "Quote received";
            case QUOTE_ACCEPTED -> "Quote accepted";
            case QUOTE_REJECTED -> "Quote rejected";
            default -> "RFQ update";
        };
        String entityId = event.quoteId() != null && !event.quoteId().isBlank()
                ? event.quoteId() : event.rfqId();
        String entityType = event.quoteId() != null && !event.quoteId().isBlank()
                ? "SupplierQuote" : "Rfq";
        String body = event.summary() != null ? event.summary()
                : title + " for RFQ " + event.rfqId();
        notificationEventService.emitInfo(
                event.recipientUserId(), type, title, body, entityType, entityId);
    }

    private static NotificationType mapType(String kind) {
        if (kind == null) {
            return NotificationType.SYSTEM;
        }
        return switch (kind.trim().toUpperCase()) {
            case "CREATED" -> NotificationType.RFQ_CREATED;
            case "SENT" -> NotificationType.RFQ_SENT;
            case "QUOTE_RECEIVED" -> NotificationType.QUOTE_RECEIVED;
            case "QUOTE_ACCEPTED" -> NotificationType.QUOTE_ACCEPTED;
            case "QUOTE_REJECTED" -> NotificationType.QUOTE_REJECTED;
            default -> NotificationType.SYSTEM;
        };
    }
}
