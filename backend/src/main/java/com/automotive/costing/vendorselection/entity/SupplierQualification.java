package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "supplier_qualification")
public class SupplierQualification extends BaseAuditableEntity {
    @Id
    @Column(name = "qualification_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String qualificationId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", length = 30, nullable = false)
    private SupplierLifecycleStatus lifecycleStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private SupplierLifecycleStatus previousStatus;
    @Column(name = "change_reason", length = 1000)
    private String changeReason;
    @Column(name = "recommendation_id", length = 36, columnDefinition = "CHAR(36)")
    private String recommendationId;
    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;
    @Column(name = "changed_by", length = 36, columnDefinition = "CHAR(36)")
    private String changedBy;
}
