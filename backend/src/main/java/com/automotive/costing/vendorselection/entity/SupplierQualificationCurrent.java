package com.automotive.costing.vendorselection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "supplier_qualification_current")
public class SupplierQualificationCurrent {
    @Id
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", length = 30, nullable = false)
    private SupplierLifecycleStatus lifecycleStatus;
    @Column(name = "qualification_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String qualificationId;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
