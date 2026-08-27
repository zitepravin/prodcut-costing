package com.automotive.costing.platform.access.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "license_history")
public class LicenseHistory {
    @Id
    @Column(name = "history_id", length = 36, nullable = false)
    private String historyId;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "license_id", length = 36)
    private String licenseId;

    @Column(name = "action", length = 40, nullable = false)
    private String action;

    @Column(name = "actor_user_id", length = 36)
    private String actorUserId;

    @Column(name = "before_json", columnDefinition = "TEXT")
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "TEXT")
    private String afterJson;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
