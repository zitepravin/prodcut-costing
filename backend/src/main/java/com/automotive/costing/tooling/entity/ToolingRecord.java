package com.automotive.costing.tooling.entity;

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
import java.time.LocalDate;

/**
 * M06 Tooling, Die &amp; Fixture Investment Costing. A per-BOM-node tooling investment
 * record - like {@code ProcessRouting}, this is transactional/per-part data, not shared
 * reference data, so it lives in its own package rather than {@code masterdata}. A BOM
 * node may have more than one tooling record over its life (a tool is replaced, or a
 * second supplier's tooling is being evaluated in parallel), so {@code bomNodeId} is
 * indexed but not unique, exactly mirroring {@code ProcessRouting}'s PRC-07 rationale.
 *
 * {@code supplierId} and {@code toolingQuoteReference} are plain string fields, not FKs -
 * M09/M10 (RFQ/Supplier Costing) don't exist yet (Phase 2). TL-06's "compare up to five
 * supplier quotes" is implemented today as comparing up to five {@code ToolingRecord}s
 * for the same BOM node (each representing one supplier's tooling proposal); once M09/M10
 * land, {@code toolingQuoteReference} is the seam a real FK would replace - the same
 * forward-reference pattern the M04 session used for the 'Buy' leaf-cost deferral.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tooling_record")
public class ToolingRecord extends BaseAuditableEntity {

    @Id
    @Column(name = "tooling_record_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String toolingRecordId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tooling_type", length = 30, nullable = false)
    private ToolingType toolingType;

    @Column(name = "description", length = 500)
    private String description;

    /** TL-03: cavity/impression count - also feeds M04 (see ProcessOperation.outputPartsPerCycle). */
    @Column(name = "cavity_count")
    private Integer cavityCount;

    @Column(name = "tool_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal toolCost;

    /** TL-04: number of hits/strokes/shots before refurbishment is required. Nullable - not every tool has a defined limit. */
    @Column(name = "tool_life_hits", precision = 18, scale = 2)
    private BigDecimal toolLifeHits;

    @Column(name = "refurbishment_cost_per_event", precision = 18, scale = 6)
    private BigDecimal refurbishmentCostPerEvent;

    /**
     * TL-04: stand-in for real MES-fed cumulative production actuals (M17/Phase 3, not
     * built yet) - manually entered/updated for now. Documented limitation, same pattern
     * as other forward references to not-yet-built modules.
     */
    @Column(name = "cumulative_volume_to_date", precision = 18, scale = 2)
    private BigDecimal cumulativeVolumeToDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership", length = 20, nullable = false)
    private ToolingOwnership ownership;

    @Column(name = "supplier_id", length = 50)
    private String supplierId;

    @Column(name = "tooling_quote_reference", length = 100)
    private String toolingQuoteReference;

    /** TL-07: asset register fields. */
    @Column(name = "plant_location", length = 100)
    private String plantLocation;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_status", length = 20, nullable = false)
    private ToolingAssetStatus assetStatus;
}
