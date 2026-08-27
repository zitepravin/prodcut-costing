package com.automotive.costing.notification.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * In-app notification delivered to a single user. Supports workflow lifecycle and WF-04
 * escalation signals; optional email fan-out is handled by {@code NotificationDeliveryService}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_notification")
public class AppNotification extends BaseAuditableEntity {

    @Id
    @Column(name = "notification_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String notificationId;

    @Column(name = "customer_id", length = 36, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "recipient_user_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 40, nullable = false)
    private NotificationType notificationType;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "body", length = 2000, nullable = false)
    private String body;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 36, columnDefinition = "CHAR(36)")
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20, nullable = false)
    private NotificationSeverity severity = NotificationSeverity.INFO;

    @Column(name = "read_flag", nullable = false)
    private boolean readFlag;

    @Column(name = "read_at")
    private Instant readAt;
}
