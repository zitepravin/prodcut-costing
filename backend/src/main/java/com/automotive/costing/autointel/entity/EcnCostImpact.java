package com.automotive.costing.autointel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "ecn_cost_impact")
public class EcnCostImpact {
    @Id @Column(name = "impact_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String impactId;
    @Column(name = "ecn_number", length = 60, nullable = false)
    private String ecnNumber;
    @Column(name = "ecn_title", length = 200)
    private String ecnTitle;
    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;
    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;
    @Column(name = "baseline_build_up_id", length = 36, columnDefinition = "CHAR(36)")
    private String baselineBuildUpId;
    @Column(name = "proposed_build_up_id", length = 36, columnDefinition = "CHAR(36)")
    private String proposedBuildUpId;
    @Column(name = "baseline_unit_cost", precision = 18, scale = 6)
    private BigDecimal baselineUnitCost;
    @Column(name = "proposed_unit_cost", precision = 18, scale = 6)
    private BigDecimal proposedUnitCost;
    @Column(name = "unit_delta", precision = 18, scale = 6)
    private BigDecimal unitDelta;
    @Column(name = "annual_volume", precision = 18, scale = 6)
    private BigDecimal annualVolume;
    @Column(name = "annual_program_delta", precision = 18, scale = 6)
    private BigDecimal annualProgramDelta;
    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "USD";
    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";
    @Column(name = "impact_summary", length = 2000)
    private String impactSummary;
    @Column(name = "notes", length = 1000)
    private String notes;
    @Column(name = "created_by", length = 36)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
