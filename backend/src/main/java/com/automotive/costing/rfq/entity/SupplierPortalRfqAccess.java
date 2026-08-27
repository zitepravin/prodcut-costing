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
@Table(name = "supplier_portal_rfq_access")
public class SupplierPortalRfqAccess extends BaseAuditableEntity {

    @Id
    @Column(name = "access_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String accessId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "rfq_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String rfqId;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "granted_by", length = 36, columnDefinition = "CHAR(36)")
    private String grantedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PortalRfqAccessStatus status = PortalRfqAccessStatus.ACTIVE;
}
