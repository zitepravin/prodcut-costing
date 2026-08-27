package com.automotive.costing.notification.service;

import com.automotive.costing.notification.entity.AppNotification;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wave E — centralized domain → in-app notification path.
 * No email logic here (Wave F / NotificationDeliveryService).
 * Never put secrets, JWTs, or passwords in title/body.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventService {

    private final NotificationService notificationService;

    @Transactional
    public AppNotification emit(
            String recipientUserId,
            NotificationType type,
            String title,
            String body,
            String entityType,
            String entityId,
            NotificationSeverity severity
    ) {
        if (recipientUserId == null || recipientUserId.isBlank()) {
            log.debug("Skip notification type={}: no recipient", type);
            return null;
        }
        if (type == null) {
            type = NotificationType.SYSTEM;
        }
        String safeTitle = truncate(title == null ? type.name() : title, 200);
        String safeBody = truncate(body == null ? "" : body, 2000);
        try {
            return notificationService.publish(
                    recipientUserId.trim(),
                    type,
                    safeTitle,
                    safeBody,
                    entityType,
                    entityId,
                    severity == null ? NotificationSeverity.INFO : severity
            );
        } catch (Exception ex) {
            log.warn("Notification emit failed type={} entity={}/{}: {}",
                    type, entityType, entityId, ex.getMessage());
            return null;
        }
    }

    @Transactional
    public AppNotification emitInfo(
            String recipientUserId,
            NotificationType type,
            String title,
            String body,
            String entityType,
            String entityId
    ) {
        return emit(recipientUserId, type, title, body, entityType, entityId, NotificationSeverity.INFO);
    }

    @Transactional
    public AppNotification emitWarning(
            String recipientUserId,
            NotificationType type,
            String title,
            String body,
            String entityType,
            String entityId
    ) {
        return emit(recipientUserId, type, title, body, entityType, entityId, NotificationSeverity.WARNING);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
