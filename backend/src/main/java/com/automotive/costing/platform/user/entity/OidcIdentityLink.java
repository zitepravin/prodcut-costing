package com.automotive.costing.platform.user.entity;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "oidc_identity_link")
public class OidcIdentityLink extends BaseAuditableEntity {

    @Id
    @Column(name = "link_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String linkId;

    @Column(name = "user_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "issuer", length = 255, nullable = false)
    private String issuer;

    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OidcLinkStatus status = OidcLinkStatus.ACTIVE;
}
