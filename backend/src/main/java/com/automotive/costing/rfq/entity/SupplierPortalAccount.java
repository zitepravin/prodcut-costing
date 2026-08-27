package com.automotive.costing.rfq.entity;

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
@Table(name = "supplier_portal_account")
public class SupplierPortalAccount extends BaseAuditableEntity {

    @Id
    @Column(name = "portal_account_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String portalAccountId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "email", length = 150, nullable = false)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PortalAccountStatus status = PortalAccountStatus.ACTIVE;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
