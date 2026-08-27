package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "supplier_delivery_metric")
public class SupplierDeliveryMetric extends BaseAuditableEntity {
    @Id
    @Column(name = "metric_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String metricId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Column(name = "otd_percent", precision = 9, scale = 4)
    private BigDecimal otdPercent;
    @Column(name = "otif_percent", precision = 9, scale = 4)
    private BigDecimal otifPercent;
    @Column(name = "late_delivery_count")
    private Integer lateDeliveryCount;
    @Column(name = "avg_delay_days", precision = 9, scale = 2)
    private BigDecimal avgDelayDays;
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", length = 20, nullable = false)
    private EvidenceStatus evidenceStatus = EvidenceStatus.PENDING;
    @Column(name = "evidence_ref", length = 255)
    private String evidenceRef;
    @Column(name = "verified_by", length = 36)
    private String verifiedBy;
    @Column(name = "verified_at")
    private Instant verifiedAt;
    @Column(name = "notes", length = 1000)
    private String notes;
}
