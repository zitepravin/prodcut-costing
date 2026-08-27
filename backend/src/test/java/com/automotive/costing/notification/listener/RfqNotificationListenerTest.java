package com.automotive.costing.notification.listener;

import com.automotive.costing.common.event.RfqLifecycleEvent;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.service.NotificationEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RfqNotificationListenerTest {

    @Mock NotificationEventService notificationEventService;
    @InjectMocks RfqNotificationListener listener;

    @Test
    void mapsCreated() {
        listener.onRfqLifecycle(new RfqLifecycleEvent(
                "CREATED", "rfq-1", null, "user-1", "RFQ created"));
        verify(notificationEventService).emitInfo(
                eq("user-1"),
                eq(NotificationType.RFQ_CREATED),
                eq("RFQ created"),
                eq("RFQ created"),
                eq("Rfq"),
                eq("rfq-1")
        );
    }

    @Test
    void mapsQuoteAcceptedUsesQuoteEntity() {
        listener.onRfqLifecycle(new RfqLifecycleEvent(
                "QUOTE_ACCEPTED", "rfq-1", "quote-9", "user-2", "accepted"));
        verify(notificationEventService).emitInfo(
                eq("user-2"),
                eq(NotificationType.QUOTE_ACCEPTED),
                eq("Quote accepted"),
                eq("accepted"),
                eq("SupplierQuote"),
                eq("quote-9")
        );
    }

    @Test
    void skipsWithoutRecipient() {
        listener.onRfqLifecycle(new RfqLifecycleEvent("CREATED", "rfq-1", null, null, "x"));
        verifyNoInteractions(notificationEventService);
    }
}
