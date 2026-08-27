package com.automotive.costing.vendorselection.entity;

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
@Table(name = "supplier_lifecycle_operation_block")
public class SupplierLifecycleOperationBlock extends BaseAuditableEntity {

    @Id
    @Column(name = "block_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String blockId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "operation_code", length = 40, nullable = false)
    private String operationCode;

    @Column(name = "lifecycle_status", length = 30, nullable = false)
    private String lifecycleStatus;

    @Column(name = "block_reason", length = 1000, nullable = false)
    private String blockReason;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "attempted_by", length = 36, columnDefinition = "CHAR(36)")
    private String attemptedBy;
}
