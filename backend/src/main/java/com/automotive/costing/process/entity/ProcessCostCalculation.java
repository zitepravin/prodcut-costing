package com.automotive.costing.process.entity;

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
import java.time.LocalDate;

/**
 * PRC-06: the persisted result of rolling up every operation's cost in a routing into a
 * single total process cost for the part. Mirrors {@code MaterialCostCalculation}'s shape
 * and rationale exactly (M03) - a new row per recalculation (never updated in place), and
 * deliberately not the full {@code cost_record} shape (M08's responsibility).
 *
 * {@code bomNodeId} is denormalized from the owning {@code ProcessRouting} so
 * {@code BomRollupEngine.LeafUnitCostResolver} can query "the latest process cost for this
 * BOM node" with the same access pattern M03 already established for material cost,
 * without an extra join through process_routing on every roll-up.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "process_cost_calculation")
public class ProcessCostCalculation extends BaseAuditableEntity {

    @Id
    @Column(name = "calculation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String calculationId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "routing_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String routingId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "total_process_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalProcessCost;

    @Column(name = "setup_cost", precision = 18, scale = 6)
    private BigDecimal setupCost;

    @Column(name = "machine_cost", precision = 18, scale = 6)
    private BigDecimal machineCost;

    @Column(name = "direct_labor_cost", precision = 18, scale = 6)
    private BigDecimal directLaborCost;

    @Column(name = "power_cost", precision = 18, scale = 6)
    private BigDecimal powerCost;

    @Column(name = "consumable_cost", precision = 18, scale = 6)
    private BigDecimal consumableCost;

    @Column(name = "inspection_cost", precision = 18, scale = 6)
    private BigDecimal inspectionCost;

    @Column(name = "external_process_cost", precision = 18, scale = 6)
    private BigDecimal externalProcessCost;

    /** JSON/text component breakdown for API retrieval */
    @Column(name = "component_breakdown", columnDefinition = "TEXT")
    private String componentBreakdown;

    @Column(name = "costing_date")
    private LocalDate costingDate;

    @Column(name = "calculation_basis", length = 1000)
    private String calculationBasis;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
