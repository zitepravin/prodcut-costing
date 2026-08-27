package com.automotive.costing.material.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The persisted result of one material cost calculation for a BOM node (MAT-01, MAT-02,
 * MAT-03, MAT-06). This is deliberately NOT the full {@code cost_record} entity described
 * in TDD Section 4.3 - a should-cost {@code cost_record} combines material + process +
 * tooling + overhead + SG&amp;A + margin, which is M08's responsibility once built. This
 * table is the material-only building block M08 will read from, per NEXT_AGENT.md's
 * explicit guidance to use judgment here rather than prematurely building the full
 * cost_record shape before the modules that populate its other cost elements exist.
 *
 * A new row is written on every recalculation (never updated in place) so MAT-07's
 * "24 months of historical...data...for trend reporting" and TDD Section 6.2's
 * idempotency/audit-traceability requirements both fall out of the same append-only
 * pattern used everywhere else in this codebase (MDM-02 master data versioning,
 * BomRollupEngine's cost history). Readers always want "the latest row for this BOM
 * node" or "the history for this material" - see MaterialCostCalculationRepository.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "material_cost_calculation")
public class MaterialCostCalculation extends BaseAuditableEntity {

    @Id
    @Column(name = "calculation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String calculationId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "material_code", length = 50, nullable = false)
    private String materialCode;

    @Column(name = "net_weight", precision = 18, scale = 6, nullable = false)
    private BigDecimal netWeight;

    /** MAT-01: fraction, 0 <= scrapPct < 1 (e.g. 0.12 = 12%), never a raw percent 0-100. */
    @Column(name = "scrap_pct", precision = 9, scale = 6, nullable = false)
    private BigDecimal scrapPct;

    @Column(name = "gross_weight", precision = 18, scale = 6, nullable = false)
    private BigDecimal grossWeight;

    @Column(name = "unit_rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_basis", length = 20, nullable = false)
    private PricingBasis pricingBasis;

    @Column(name = "scrap_recovery_credit", precision = 18, scale = 6, nullable = false)
    private BigDecimal scrapRecoveryCredit;

    /** MAT-01/02/03 final result: (grossWeight x unitRate) - scrapRecoveryCredit. */
    @Column(name = "material_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal materialCost;

    /** Human-readable trace of exactly which inputs/formula produced materialCost. */
    @Column(name = "calculation_basis", length = 500)
    private String calculationBasis;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
