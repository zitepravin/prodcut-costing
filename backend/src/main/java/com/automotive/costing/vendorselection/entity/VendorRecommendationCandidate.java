package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendor_recommendation_candidate")
public class VendorRecommendationCandidate extends BaseAuditableEntity {
    @Id
    @Column(name = "candidate_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String candidateId;
    @Column(name = "recommendation_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String recommendationId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;
    @Column(name = "rank_position", nullable = false)
    private int rankPosition;
    @Column(name = "eligible", nullable = false)
    private boolean eligible;
    @Column(name = "overall_score", precision = 9, scale = 4)
    private BigDecimal overallScore;
    @Column(name = "tco_total", precision = 18, scale = 6)
    private BigDecimal tcoTotal;
    @Column(name = "tco_currency", length = 3)
    private String tcoCurrency;
    @Column(name = "tco_data_complete")
    private Boolean tcoDataComplete;
    @Column(name = "failed_gates", length = 500)
    private String failedGates;
    @Column(name = "strengths", length = 1000)
    private String strengths;
    @Column(name = "weaknesses", length = 1000)
    private String weaknesses;
    @Column(name = "evidence_notes", length = 1000)
    private String evidenceNotes;

    @Column(name = "lifecycle_status", length = 30)
    private String lifecycleStatus;

    @Column(name = "lifecycle_blocked", nullable = false)
    private boolean lifecycleBlocked = false;
}
