package com.automotive.costing.notification.service;

import com.automotive.costing.notification.config.NotificationProperties;
import com.automotive.costing.notification.entity.AppNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Bridges in-app publish → email queue (Wave F).
 * Controllers must not call SMTP directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryService {

    private final NotificationProperties properties;
    private final EmailDeliveryQueueService emailDeliveryQueueService;

    public void onPublished(AppNotification notification) {
        if (!properties.isEmailEnabled()) {
            log.debug("Email delivery disabled; notification {} stored in-app only",
                    notification.getNotificationId());
            return;
        }
        emailDeliveryQueueService.enqueueFromNotification(notification, null)
                .ifPresentOrElse(
                        d -> log.debug("Queued email delivery {} for notification {}",
                                d.getDeliveryId(), notification.getNotificationId()),
                        () -> log.debug("No email queued for notification {}",
                                notification.getNotificationId())
                );
    }
}
