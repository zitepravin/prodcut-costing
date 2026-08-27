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
@Table(name = "supplier_risk")
public class SupplierRisk extends BaseAuditableEntity {
    @Id
    @Column(name = "risk_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String riskId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", length = 40, nullable = false)
    private RiskCategory riskCategory;
    @Column(name = "risk_title", length = 200, nullable = false)
    private String riskTitle;
    @Column(name = "description", length = 2000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20, nullable = false)
    private RiskSeverity severity;
    @Enumerated(EnumType.STRING)
    @Column(name = "likelihood", length = 20)
    private RiskLikelihood likelihood;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RiskStatus status = RiskStatus.OPEN;
    @Column(name = "mitigation_plan", length = 2000)
    private String mitigationPlan;
    @Enumerated(EnumType.STRING)
    @Column(name = "residual_severity", length = 20)
    private RiskSeverity residualSeverity;
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", length = 20, nullable = false)
    private EvidenceStatus evidenceStatus = EvidenceStatus.PENDING;
    @Column(name = "evidence_ref", length = 255)
    private String evidenceRef;
    @Column(name = "identified_at", nullable = false)
    private Instant identifiedAt;
    @Column(name = "identified_by", length = 36, columnDefinition = "CHAR(36)")
    private String identifiedBy;
    @Column(name = "closed_at")
    private Instant closedAt;
    @Column(name = "closed_by", length = 36, columnDefinition = "CHAR(36)")
    private String closedBy;
}
