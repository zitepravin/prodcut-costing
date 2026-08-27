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
@Table(name = "vendor_recommendation_decision")
public class VendorRecommendationDecision extends BaseAuditableEntity {

    @Id
    @Column(name = "decision_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String decisionId;

    @Column(name = "recommendation_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String recommendationId;

    @Column(name = "explanation_id", length = 36, columnDefinition = "CHAR(36)")
    private String explanationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30, nullable = false)
    private RecommendationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30, nullable = false)
    private RecommendationStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_action", length = 30, nullable = false)
    private RecommendationDecisionAction decisionAction;

    @Column(name = "recommended_supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String recommendedSupplierId;

    @Column(name = "override_supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String overrideSupplierId;

    @Column(name = "decision_comment", length = 2000)
    private String decisionComment;

    @Column(name = "override_reason", length = 2000)
    private String overrideReason;

    @Column(name = "decided_by", length = 36, columnDefinition = "CHAR(36)")
    private String decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;
}
