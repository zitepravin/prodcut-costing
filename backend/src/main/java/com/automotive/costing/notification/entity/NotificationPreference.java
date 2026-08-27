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
@Table(name = "notification_preference")
public class NotificationPreference {

    @Id
    @Column(name = "preference_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String preferenceId;

    @Column(name = "user_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "customer_id", length = 36, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "module_code", length = 40, nullable = false)
    private String moduleCode;

    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;
}
