package com.automotive.costing.tooling.entity;

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
 * TL-02/TL-04: the persisted result of amortizing one {@code ToolingRecord}'s investment
 * over a program's lifetime volume, including any projected refurbishment cost. Mirrors
 * {@code MaterialCostCalculation}/{@code ProcessCostCalculation}'s shape and rationale
 * exactly - append-only, deliberately not the full {@code cost_record} shape (M08's
 * responsibility).
 *
 * {@code amortizedCostPerPiece} is the full per-piece figure (base amortization +
 * refurbishment amortization); {@code includedInPiecePrice} carries TL-05's ownership
 * rule forward so a consumer never has to re-derive "should this actually count toward
 * the piece price" from the tooling record's ownership flag separately.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tooling_cost_calculation")
public class ToolingCostCalculation extends BaseAuditableEntity {

    @Id
    @Column(name = "calculation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String calculationId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "tooling_record_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String toolingRecordId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "program_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "base_amortized_cost_per_piece", precision = 18, scale = 6, nullable = false)
    private BigDecimal baseAmortizedCostPerPiece;

    @Column(name = "refurbishment_events_projected")
    private Integer refurbishmentEventsProjected;

    @Column(name = "refurbishment_cost_per_piece", precision = 18, scale = 6)
    private BigDecimal refurbishmentCostPerPiece;

    @Column(name = "total_amortized_cost_per_piece", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalAmortizedCostPerPiece;

    /** TL-05: false when the owning ToolingRecord is CUSTOMER-owned - excluded from piece price, tracked separately. */
    @Column(name = "included_in_piece_price", nullable = false)
    private boolean includedInPiecePrice;

    @Column(name = "calculation_basis", length = 1000)
    private String calculationBasis;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
