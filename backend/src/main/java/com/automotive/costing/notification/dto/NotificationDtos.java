package com.automotive.costing.notification.dto;

import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;

import java.time.Instant;

public class NotificationDtos {

    public record Response(
            String notificationId,
            NotificationType notificationType,
            String title,
            String body,
            String entityType,
            String entityId,
            NotificationSeverity severity,
            boolean readFlag,
            Instant readAt,
            Instant createdAt
    ) {
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}
