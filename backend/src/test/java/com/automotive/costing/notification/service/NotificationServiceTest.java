package com.automotive.costing.notification.service;

import com.automotive.costing.notification.entity.AppNotification;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.repository.AppNotificationRepository;
import com.automotive.costing.platform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private AppNotificationRepository repository;
    @Mock
    private NotificationDeliveryService deliveryService;

    @InjectMocks
    private NotificationService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId("user-1");
        user.setUsername("engineer");
    }

    @Test
    void publishPersistsAndDelivers() {
        when(repository.save(any(AppNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        AppNotification n = service.publish(
                "user-1",
                NotificationType.APPROVAL_ESCALATED,
                "Escalated",
                "Body",
                "ApprovalStepInstance",
                "step-1",
                NotificationSeverity.CRITICAL);

        assertEquals("user-1", n.getRecipientUserId());
        assertEquals(NotificationType.APPROVAL_ESCALATED, n.getNotificationType());
        assertFalse(n.isReadFlag());
        verify(deliveryService).onPublished(n);
    }

    @Test
    void markReadOnlyForRecipient() {
        AppNotification n = new AppNotification();
        n.setNotificationId("n-1");
        n.setRecipientUserId("user-1");
        n.setReadFlag(false);
        n.setNotificationType(NotificationType.SYSTEM);
        n.setTitle("t");
        n.setBody("b");
        n.setSeverity(NotificationSeverity.INFO);
        when(repository.findById("n-1")).thenReturn(Optional.of(n));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.markRead(user, "n-1");
        assertTrue(resp.readFlag());
        assertNotNull(resp.readAt());
    }

    @Test
    void unreadCountDelegates() {
        when(repository.countByRecipientUserIdAndReadFlag("user-1", false)).thenReturn(3L);
        assertEquals(3L, service.unreadCount(user).unreadCount());
    }

    @Test
    void listUnreadFilters() {
        AppNotification n = new AppNotification();
        n.setNotificationId("n-2");
        n.setRecipientUserId("user-1");
        n.setNotificationType(NotificationType.WORKFLOW_SUBMITTED);
        n.setTitle("t");
        n.setBody("b");
        n.setSeverity(NotificationSeverity.INFO);
        n.setReadFlag(false);
        when(repository.findByRecipientUserIdAndReadFlagOrderByCreatedAtDesc(eq("user-1"), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<?> page = service.listForUser(user, true, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }
}
