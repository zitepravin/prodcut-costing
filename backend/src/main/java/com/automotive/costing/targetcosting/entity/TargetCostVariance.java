package com.automotive.costing.targetcosting.entity;

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
@Table(name = "target_cost_variance")
public class TargetCostVariance extends BaseAuditableEntity {

    @Id
    @Column(name = "variance_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String varianceId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "target_cost_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String targetCostId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "program_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "target_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "actual_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal actualAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "actual_source", length = 30, nullable = false)
    private ActualCostSource actualSource;

    @Column(name = "actual_source_id", length = 36, columnDefinition = "CHAR(36)")
    private String actualSourceId;

    @Column(name = "absolute_variance", precision = 18, scale = 6, nullable = false)
    private BigDecimal absoluteVariance;

    @Column(name = "percentage_variance", precision = 18, scale = 6, nullable = false)
    private BigDecimal percentageVariance;

    @Column(name = "exceeds_threshold", nullable = false)
    private boolean exceedsThreshold;

    @Column(name = "threshold_pct", precision = 9, scale = 6)
    private BigDecimal thresholdPct;

    @Column(name = "calculation_basis", length = 1000)
    private String calculationBasis;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
