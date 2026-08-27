package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendor_scorecard_result")
public class VendorScorecardResult extends BaseAuditableEntity {
    @Id
    @Column(name = "result_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String resultId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "template_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String templateId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;
    @Column(name = "bom_node_id", length = 36, columnDefinition = "CHAR(36)")
    private String bomNodeId;
    @Column(name = "overall_score", precision = 9, scale = 4, nullable = false)
    private BigDecimal overallScore;
    @Column(name = "max_score", precision = 9, scale = 4, nullable = false)
    private BigDecimal maxScore = new BigDecimal("100.0000");
    @Lob
    @Column(name = "category_scores_json", columnDefinition = "LONGTEXT")
    private String categoryScoresJson;
    @Column(name = "notes", length = 1000)
    private String notes;
    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;
    @Column(name = "scored_by", length = 36, columnDefinition = "CHAR(36)")
    private String scoredBy;
}
