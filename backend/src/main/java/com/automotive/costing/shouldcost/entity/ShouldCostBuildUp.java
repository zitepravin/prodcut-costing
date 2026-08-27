package com.automotive.costing.shouldcost.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
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
 * SC-01/03/06: the persisted, itemized should-cost build-up - the first entity in the
 * system that actually matches the SRS Appendix D nine-element cost structure field for
 * field (material, process, tooling, packaging/freight, overhead, SG&amp;A, margin,
 * total). Everything before this module (M03/M04/M06's calculation records) computed one
 * element each; this is where they get assembled into the single figure the SRS describes
 * as the point of the whole system.
 *
 * Unlike {@code MaterialCostCalculation}/{@code ProcessCostCalculation}/
 * {@code ToolingCostCalculation} (where "latest wins" is always the right read pattern),
 * SC-06 requires multiple named scenarios to coexist for the same BOM node
 * simultaneously ("save and label multiple should-cost scenarios per part, e.g. 'Current
 * Design', 'Lightweighted Alternative'") - so {@code scenarioLabel} is part of this
 * entity's identity, not just metadata, and callers must not assume "most recent row for
 * this bomNodeId" is the only one that matters.
 *
 * {@code locked} implements SC-01/4.8.5's "a should-cost estimate is locked (read-only)
 * once linked to a formal supplier negotiation record" - the flag and the guard exist now
 * (see {@code ShouldCostService.lock()}), but nothing calls it automatically yet since
 * M09/M10 (RFQ/Supplier Costing, the source of a "formal negotiation record") don't exist.
 * The seam is real; the automatic trigger is a forward reference, same pattern used
 * throughout this project for not-yet-built modules.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "should_cost_build_up")
public class ShouldCostBuildUp extends BaseAuditableEntity {

    @Id
    @Column(name = "build_up_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String buildUpId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    /**
     * Denormalized from BomNode.programId (V22) for efficient program-scoped reporting.
     * Set on create; not a substitute for bom_node ownership.
     */
    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;

    /** Optional A1 costing variant used when this build-up was generated. */
    @Column(name = "costing_variant_id", length = 36, columnDefinition = "CHAR(36)")
    private String costingVariantId;

    /** SC-06: distinguishes concurrent scenarios for the same part. Not unique per bomNodeId - multiple versions of the same label may exist over time. */
    @Column(name = "scenario_label", length = 100, nullable = false)
    private String scenarioLabel;

    @Column(name = "material_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal materialCost;

    @Column(name = "process_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal processCost;

    @Column(name = "tooling_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal toolingCost;

    /** M07 Packaging & Logistics: populated from latest PackagingLogisticsCostCalculation for the BOM node (zero when none calculated). */
    @Column(name = "packaging_freight_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal packagingFreightCost;

    @Column(name = "overhead_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal overheadCost;

    @Column(name = "sga_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal sgaCost;

    @Column(name = "profit_margin", precision = 18, scale = 6, nullable = false)
    private BigDecimal profitMargin;

    @Column(name = "total_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalCost;

    /** SC-07: true if any consumed material/process/tooling calculation predates the staleness threshold. */
    @Column(name = "stale_inputs_flag", nullable = false)
    private boolean staleInputsFlag;

    @Column(name = "locked", nullable = false)
    private boolean locked;

    /**
     * WF-06/SRS 4.14.5: DRAFT until submitted for approval, SUBMITTED while a workflow is
     * in progress, APPROVED or REJECTED once it completes - a REJECTED outcome reverts
     * this back to DRAFT (see {@code ShouldCostApprovalListener}), satisfying "a rejected
     * cost record returns to 'Draft' status." Defaults to DRAFT on every new
     * {@code generate()} call (SC-06's scenarios are all independently draftable).
     */
    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "approval_status", length = 20, nullable = false)
    private ApprovalStatus approvalStatus;

    @Column(name = "calculation_basis", length = 2000)
    private String calculationBasis;

    @Column(name = "fx_source_currency", length = 3, columnDefinition = "CHAR(3)")
    private String fxSourceCurrency;
    @Column(name = "fx_target_currency", length = 3, columnDefinition = "CHAR(3)")
    private String fxTargetCurrency;
    @Column(name = "fx_applied_rate", precision = 18, scale = 8)
    private BigDecimal fxAppliedRate;
    @Column(name = "fx_rate_effective_date")
    private LocalDate fxRateEffectiveDate;
    @Column(name = "fx_as_of_date")
    private LocalDate fxAsOfDate;
    @Column(name = "fx_conversion_method", length = 20)
    private String fxConversionMethod;
    @Column(name = "fx_rate_source", length = 100)
    private String fxRateSource;
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}

