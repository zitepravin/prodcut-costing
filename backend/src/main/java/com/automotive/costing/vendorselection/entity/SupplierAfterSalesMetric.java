package com.automotive.costing.vendorselection.entity;
import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "supplier_after_sales_metric")
public class SupplierAfterSalesMetric extends BaseAuditableEntity {
    @Id @Column(name = "metric_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)") private String metricId;
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)") private String supplierId;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end", nullable = false) private LocalDate periodEnd;
    @Column(name = "response_time_hours", precision = 12, scale = 2) private BigDecimal responseTimeHours;
    @Column(name = "resolution_time_hours", precision = 12, scale = 2) private BigDecimal resolutionTimeHours;
    @Column(name = "emergency_support_flag") private Boolean emergencySupportFlag;
    @Column(name = "field_support_flag") private Boolean fieldSupportFlag;
    @Column(name = "spare_parts_available") private Boolean sparePartsAvailable;
    @Column(name = "local_support_flag") private Boolean localSupportFlag;
    @Column(name = "unresolved_complaints") private Integer unresolvedComplaints;
    @Column(name = "avg_closure_days", precision = 12, scale = 2) private BigDecimal avgClosureDays;
    @Column(name = "service_sla_met_percent", precision = 7, scale = 4) private BigDecimal serviceSlaMetPercent;
    @Enumerated(EnumType.STRING) @Column(name = "evidence_status", length = 20, nullable = false) private EvidenceStatus evidenceStatus = EvidenceStatus.PENDING;
    @Column(name = "evidence_ref", length = 255) private String evidenceRef;
    @Column(name = "verified_by", length = 36) private String verifiedBy;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "notes", length = 1000) private String notes;
}
