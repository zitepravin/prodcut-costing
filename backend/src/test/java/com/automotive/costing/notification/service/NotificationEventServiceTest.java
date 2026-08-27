package com.automotive.costing.notification.service;

import com.automotive.costing.notification.entity.AppNotification;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventServiceTest {

    @Mock NotificationService notificationService;
    @InjectMocks NotificationEventService eventService;

    @Test
    void emitSkipsBlankRecipient() {
        assertNull(eventService.emitInfo(null, NotificationType.RFQ_CREATED, "t", "b", "Rfq", "id"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void emitDelegatesToNotificationService() {
        AppNotification n = new AppNotification();
        n.setNotificationId("n1");
        when(notificationService.publish(anyString(), any(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(n);

        AppNotification out = eventService.emit(
                "user-1",
                NotificationType.QUOTE_ACCEPTED,
                "Quote accepted",
                "Details",
                "SupplierQuote",
                "q1",
                NotificationSeverity.INFO
        );
        assertNotNull(out);
        verify(notificationService).publish(
                eq("user-1"),
                eq(NotificationType.QUOTE_ACCEPTED),
                eq("Quote accepted"),
                eq("Details"),
                eq("SupplierQuote"),
                eq("q1"),
                eq(NotificationSeverity.INFO)
        );
    }

    @Test
    void emitTruncatesLongBody() {
        when(notificationService.publish(anyString(), any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new AppNotification());
        String longBody = "x".repeat(5000);
        eventService.emitInfo("u1", NotificationType.SYSTEM_ALERT, "Alert", longBody, null, null);
        verify(notificationService).publish(
                eq("u1"),
                eq(NotificationType.SYSTEM_ALERT),
                eq("Alert"),
                argThat(b -> b.length() == 2000),
                isNull(),
                isNull(),
                eq(NotificationSeverity.INFO)
        );
    }
}
