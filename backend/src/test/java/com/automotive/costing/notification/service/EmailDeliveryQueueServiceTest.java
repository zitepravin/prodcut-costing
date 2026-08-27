package com.automotive.costing.notification.service;

import com.automotive.costing.notification.config.NotificationProperties;
import com.automotive.costing.notification.entity.AppNotification;
import com.automotive.costing.notification.entity.DeliveryStatus;
import com.automotive.costing.notification.entity.NotificationDelivery;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.repository.NotificationDeliveryRepository;
import com.automotive.costing.notification.repository.NotificationTemplateRepository;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailDeliveryQueueServiceTest {

    @Mock NotificationDeliveryRepository deliveryRepository;
    @Mock NotificationTemplateRepository templateRepository;
    @Mock NotificationPreferenceService preferenceService;
    @Mock EmailTemplateRenderer renderer;
    @Mock UserRepository userRepository;
    @Mock NotificationProperties properties;
    @Mock ObjectProvider<JavaMailSender> mailSenderProvider;

    @InjectMocks EmailDeliveryQueueService queueService;

    @BeforeEach
    void setUp() {
        when(properties.isEmailEnabled()).thenReturn(true);
        when(preferenceService.isEmailEnabled(anyString(), anyString(), anyString())).thenReturn(true);
        when(templateRepository.findFirstByEventTypeAndLanguageCodeAndActiveFlagTrue(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(renderer.render(anyString(), any())).thenAnswer(i -> i.getArgument(0));
        when(deliveryRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void enqueueCreatesQueuedDelivery() {
        User u = new User();
        u.setUserId("u1");
        u.setUsername("buyer");
        u.setEmail("buyer@example.com");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        AppNotification n = new AppNotification();
        n.setNotificationId("n-1");
        n.setRecipientUserId("u1");
        n.setNotificationType(NotificationType.PURCHASE_ORDER_APPROVAL_REQUIRED);
        n.setTitle("Need approval");
        n.setBody("Please approve");

        Optional<NotificationDelivery> d = queueService.enqueueFromNotification(n, "PURCHASE");
        assertTrue(d.isPresent());
        assertEquals(DeliveryStatus.QUEUED, d.get().getStatus());
        assertEquals("notif:n-1", d.get().getIdempotencyKey());
        assertEquals("buyer@example.com", d.get().getRecipientEmail());
    }

    @Test
    void idempotentSecondEnqueueReturnsExisting() {
        NotificationDelivery existing = new NotificationDelivery();
        existing.setDeliveryId("d1");
        existing.setIdempotencyKey("notif:n-2");
        existing.setStatus(DeliveryStatus.SENT);
        when(deliveryRepository.findByIdempotencyKey("notif:n-2")).thenReturn(Optional.of(existing));

        AppNotification n = new AppNotification();
        n.setNotificationId("n-2");
        n.setRecipientUserId("u1");
        n.setNotificationType(NotificationType.RFQ_CREATED);
        n.setTitle("t");
        n.setBody("b");

        Optional<NotificationDelivery> d = queueService.enqueueFromNotification(n, null);
        assertTrue(d.isPresent());
        assertEquals("d1", d.get().getDeliveryId());
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void respectsEmailOptOut() {
        when(preferenceService.isEmailEnabled(eq("u1"), eq("PURCHASE"), anyString())).thenReturn(false);
        AppNotification n = new AppNotification();
        n.setNotificationId("n-3");
        n.setRecipientUserId("u1");
        n.setNotificationType(NotificationType.PURCHASE_REQUEST_SUBMITTED);
        n.setTitle("t");
        n.setBody("b");
        assertTrue(queueService.enqueueFromNotification(n, "PURCHASE").isEmpty());
    }
}
