package com.automotive.costing.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_delivery")
public class NotificationDelivery {

    @Id
    @Column(name = "delivery_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String deliveryId;

    @Column(name = "notification_id", length = 36, columnDefinition = "CHAR(36)")
    private String notificationId;

    @Column(name = "idempotency_key", length = 120, nullable = false)
    private String idempotencyKey;

    @Column(name = "recipient_user_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String recipientUserId;

    @Column(name = "recipient_email", length = 150)
    private String recipientEmail;

    @Column(name = "template_code", length = 80)
    private String templateCode;

    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;

    @Column(name = "subject_rendered", length = 500)
    private String subjectRendered;

    @Column(name = "body_rendered", length = 4000)
    private String bodyRendered;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "QUEUED";

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;
}
