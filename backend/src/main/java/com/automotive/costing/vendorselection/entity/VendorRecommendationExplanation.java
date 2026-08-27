package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendor_recommendation_explanation")
public class VendorRecommendationExplanation extends BaseAuditableEntity {

    @Id
    @Column(name = "explanation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String explanationId;

    @Column(name = "recommendation_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String recommendationId;

    @Column(name = "recommended_supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String recommendedSupplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", length = 20)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "data_used_summary", length = 2000)
    private String dataUsedSummary;

    @Column(name = "eligibility_summary", length = 2000)
    private String eligibilitySummary;

    @Column(name = "score_summary", length = 2000)
    private String scoreSummary;

    @Column(name = "tco_summary", length = 2000)
    private String tcoSummary;

    @Column(name = "strengths_summary", length = 2000)
    private String strengthsSummary;

    @Column(name = "weaknesses_summary", length = 2000)
    private String weaknessesSummary;

    @Column(name = "risk_summary", length = 2000)
    private String riskSummary;

    @Column(name = "missing_data_summary", length = 2000)
    private String missingDataSummary;

    @Column(name = "cost_variance_notes", length = 2000)
    private String costVarianceNotes;

    @Column(name = "negotiation_opportunities", length = 2000)
    private String negotiationOpportunities;

    @Column(name = "evidence_notes", length = 2000)
    private String evidenceNotes;

    @Column(name = "decision_boundary_note", length = 1000, nullable = false)
    private String decisionBoundaryNote;

    @Lob
    @Column(name = "fact_json", columnDefinition = "LONGTEXT")
    private String factJson;

    @Lob
    @Column(name = "calculated_json", columnDefinition = "LONGTEXT")
    private String calculatedJson;

    @Lob
    @Column(name = "inference_json", columnDefinition = "LONGTEXT")
    private String inferenceJson;

    @Lob
    @Column(name = "missing_json", columnDefinition = "LONGTEXT")
    private String missingJson;
}
