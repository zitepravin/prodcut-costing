package com.automotive.costing.vendorselection.entity;
import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "supplier_performance_feedback")
public class SupplierPerformanceFeedback extends BaseAuditableEntity {
    @Id @Column(name = "feedback_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)") private String feedbackId;
    @Column(name = "customer_id", length = 36, columnDefinition = "CHAR(36)") private String customerId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)") private String supplierId;
    @Column(name = "bom_node_id", length = 36, columnDefinition = "CHAR(36)") private String bomNodeId;
    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)") private String programId;
    @Column(name = "feedback_date", nullable = false) private LocalDate feedbackDate;
    @Enumerated(EnumType.STRING) @Column(name = "feedback_type", length = 40, nullable = false) private PerformanceFeedbackType feedbackType;
    @Column(name = "period_start") private LocalDate periodStart;
    @Column(name = "period_end") private LocalDate periodEnd;
    @Column(name = "metric_code", length = 40) private String metricCode;
    @Column(name = "metric_value", precision = 18, scale = 6) private BigDecimal metricValue;
    @Column(name = "unit", length = 20) private String unit;
    @Column(name = "narrative", length = 2000) private String narrative;
    @Column(name = "source_reference", length = 255) private String sourceReference;
    @Enumerated(EnumType.STRING) @Column(name = "evidence_status", length = 20, nullable = false) private EvidenceStatus evidenceStatus = EvidenceStatus.PENDING;
    @Column(name = "evidence_ref", length = 255) private String evidenceRef;
    @Column(name = "verified_by", length = 36) private String verifiedBy;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "applied_to_history", nullable = false) private boolean appliedToHistory;
    @Column(name = "applied_at") private Instant appliedAt;
    @Column(name = "history_metric_id", length = 36, columnDefinition = "CHAR(36)") private String historyMetricId;
    @Column(name = "history_metric_kind", length = 20) private String historyMetricKind;
    @Column(name = "notes", length = 1000) private String notes;
}
