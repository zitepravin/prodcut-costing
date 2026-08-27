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
@Table(name = "vendor_recommendation")
public class VendorRecommendation extends BaseAuditableEntity {
    @Id
    @Column(name = "recommendation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String recommendationId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "sourcing_event_code", length = 100)
    private String sourcingEventCode;
    @Column(name = "bom_node_id", length = 36, columnDefinition = "CHAR(36)")
    private String bomNodeId;
    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;
    @Column(name = "template_id", length = 36, columnDefinition = "CHAR(36)")
    private String templateId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private RecommendationStatus status = RecommendationStatus.DRAFT;
    @Column(name = "recommended_supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String recommendedSupplierId;
    @Lob
    @Column(name = "ranking_json", columnDefinition = "LONGTEXT")
    private String rankingJson;
    @Column(name = "rationale", length = 2000)
    private String rationale;
    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", length = 20)
    private ConfidenceLevel confidenceLevel;
    @Column(name = "missing_data_summary", length = 1000)
    private String missingDataSummary;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "submitted_by", length = 36, columnDefinition = "CHAR(36)")
    private String submittedBy;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "decided_by", length = 36, columnDefinition = "CHAR(36)")
    private String decidedBy;
    @Column(name = "decision_comment", length = 2000)
    private String decisionComment;
    @Column(name = "override_reason", length = 2000)
    private String overrideReason;
}
