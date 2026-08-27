package com.automotive.costing.platform.access.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
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
@Table(name = "user_invitation")
public class UserInvitation extends BaseAuditableEntity {

    @Id
    @Column(name = "invitation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String invitationId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "user_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "email", length = 150, nullable = false)
    private String email;

    /** SHA-256 hex of the one-time token (never store plaintext). */
    @Column(name = "token_hash", length = 64, nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    /** PENDING / ACCEPTED / EXPIRED / REVOKED */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";
}
