package com.automotive.costing.packaging.entity;

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
 * Append-only result of an M07 packaging & logistics cost calculation.
 * Consumed by Should-Cost (SC-01 packaging_freight_cost element) via
 * "latest calculation for bom_node_id".
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "packaging_logistics_cost_calculation")
public class PackagingLogisticsCostCalculation extends BaseAuditableEntity {

    @Id
    @Column(name = "calculation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String calculationId;

    @Column(name = "profile_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String profileId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "program_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "packaging_cost_per_piece", precision = 18, scale = 6, nullable = false)
    private BigDecimal packagingCostPerPiece;

    @Column(name = "freight_cost_per_piece", precision = 18, scale = 6, nullable = false)
    private BigDecimal freightCostPerPiece;

    @Column(name = "duty_cost_per_piece", precision = 18, scale = 6, nullable = false)
    private BigDecimal dutyCostPerPiece;

    @Column(name = "total_packaging_freight_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalPackagingFreightCost;

    @Column(name = "calculation_basis", length = 2000)
    private String calculationBasis;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
