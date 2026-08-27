package com.automotive.costing.mixedcost.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * P2-03: Named mixed-cost version for a BOM node (weighted multi-source unit cost).
 * Status: DRAFT | CALCULATED | FROZEN | SUPERSEDED.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mixed_cost_version")
public class MixedCostVersion extends BaseAuditableEntity {

    @Id
    @Column(name = "mixed_cost_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String mixedCostId;

    @Column(name = "program_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "version_label", length = 120, nullable = false)
    private String versionLabel;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";

    @Column(name = "reporting_currency", length = 3, columnDefinition = "CHAR(3)")
    private String reportingCurrency;

    @Column(name = "blended_unit_total", precision = 18, scale = 6)
    private BigDecimal blendedUnitTotal;

    @Column(name = "blended_status", length = 20)
    private String blendedStatus;

    @Column(name = "ratio_sum_check", precision = 10, scale = 4)
    private BigDecimal ratioSumCheck;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "calculated_at")
    private Instant calculatedAt;

    @Column(name = "frozen_at")
    private Instant frozenAt;
}
