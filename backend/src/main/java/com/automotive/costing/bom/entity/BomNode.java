package com.automotive.costing.bom.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Recursive, self-referencing BOM node (BOM-01: unlimited levels; DATABASE_SCHEMA.md
 * Section 3). Models everything from the top-level vehicle program node down to a
 * single raw-material line item.
 *
 * parentNodeId is a plain FK column (not a JPA @ManyToOne self-reference) deliberately -
 * for a 20,000-node BOM, eagerly navigable parent/child object graphs invite N+1 query
 * storms. Tree traversal for roll-up goes through BomNodeRepository's recursive CTE
 * query instead (TDD Section 6.3: "avoid N+1 database queries during roll-up: batch-load
 * all nodes ... in one query, then perform the roll-up in memory").
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bom_node")
public class BomNode extends BaseAuditableEntity {

    @Id
    @Column(name = "bom_node_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    /** Tenant owner — set from CustomerContext; never trust client alone. */
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "parent_node_id", length = 36, columnDefinition = "CHAR(36)")
    private String parentNodeId; // nullable for root

    @Column(name = "program_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "part_number", length = 50, nullable = false)
    private String partNumber;

    @Column(name = "revision", length = 20)
    private String revision;

    @Column(name = "quantity_per", precision = 18, scale = 6)
    private BigDecimal quantityPer; // BOM-05: must be populated before roll-up executes

    @Column(name = "uom", length = 10)
    private String uom; // BOM-05: must be populated before roll-up executes

    @Enumerated(EnumType.STRING)
    @Column(name = "make_buy_flag", length = 10)
    private MakeBuyFlag makeBuyFlag; // BOM-07

    @Column(name = "ecn_reference", length = 50)
    private String ecnReference; // required before status can move to RELEASED

    @Column(name = "net_weight", precision = 18, scale = 6)
    private BigDecimal netWeight;

    @Column(name = "scrap_pct", precision = 9, scale = 6)
    private BigDecimal scrapPct;

    /**
     * MAT-05/BOM-07: which material_master code this leaf node consumes, if any (null for
     * assembly-level nodes and for 'Make' nodes whose cost is process-costed rather than
     * material-costed). This is the linkage BomRollupEngine.LeafUnitCostResolver uses to
     * resolve a real material cost via MaterialCostCalculationRepository, and the linkage
     * MaterialRateRecalculationListener uses to find affected nodes when a rate publishes.
     */
    @Column(name = "material_code", length = 50)
    private String materialCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BomNodeStatus status;

    @Column(name = "what_if_branch_flag", nullable = false)
    private boolean whatIfBranchFlag; // BOM-04

    /**
     * The rolled-up extended cost of this node's subtree (unit cost x quantity-per,
     * summed recursively - BOM-02, SRS 4.2.5 business rule). Persisted rather than
     * computed on every read so dashboard/report queries (M16) don't have to re-walk
     * the tree; recalculated by BomRollupEngine whenever a leaf-level cost changes.
     */
    @Column(name = "rolled_up_cost", precision = 18, scale = 6)
    private BigDecimal rolledUpCost;

    /**
     * Manual override for rolled_up_cost. SRS 4.2.5: "never manually overridden without
     * an explicit flag and justification note" - overrideFlag + overrideJustification
     * are the enforcement mechanism; the service layer rejects manualCostOverride writes
     * that don't supply a justification.
     */
    @Column(name = "manual_cost_override", precision = 18, scale = 6)
    private BigDecimal manualCostOverride;

    @Column(name = "override_flag", nullable = false)
    private boolean overrideFlag;

    @Column(name = "override_justification", length = 500)
    private String overrideJustification;
}
