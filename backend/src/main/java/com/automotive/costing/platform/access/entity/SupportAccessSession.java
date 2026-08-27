package com.automotive.costing.platform.access.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "support_access_session")
public class SupportAccessSession {
    @Id @Column(name = "session_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String sessionId;
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "requested_by_user_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String requestedByUserId;
    @Column(name = "approved_by_user_id", length = 36, columnDefinition = "CHAR(36)")
    private String approvedByUserId;
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";
    @Column(name = "access_mode", length = 20, nullable = false)
    private String accessMode = "READ_ONLY";
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;
    @Column(name = "reason", length = 500)
    private String reason;
    @Column(name = "notes", length = 1000)
    private String notes;
}
