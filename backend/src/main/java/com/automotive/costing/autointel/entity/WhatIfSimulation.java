package com.automotive.costing.autointel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "whatif_simulation")
public class WhatIfSimulation {
    @Id @Column(name = "simulation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String simulationId;
    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;
    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;
    @Column(name = "label", length = 200, nullable = false)
    private String label;
    @Column(name = "baseline_unit_cost", precision = 18, scale = 6)
    private BigDecimal baselineUnitCost;
    @Column(name = "baseline_currency", length = 3)
    private String baselineCurrency;
    @Column(name = "material_pct_change", precision = 9, scale = 6)
    private BigDecimal materialPctChange;
    @Column(name = "volume_pct_change", precision = 9, scale = 6)
    private BigDecimal volumePctChange;
    @Column(name = "fx_pct_change", precision = 9, scale = 6)
    private BigDecimal fxPctChange;
    @Column(name = "process_pct_change", precision = 9, scale = 6)
    private BigDecimal processPctChange;
    @Column(name = "tooling_pct_change", precision = 9, scale = 6)
    private BigDecimal toolingPctChange;
    @Column(name = "simulated_unit_cost", precision = 18, scale = 6)
    private BigDecimal simulatedUnitCost;
    @Column(name = "unit_delta", precision = 18, scale = 6)
    private BigDecimal unitDelta;
    @Column(name = "annual_volume", precision = 18, scale = 6)
    private BigDecimal annualVolume;
    @Column(name = "annual_delta", precision = 18, scale = 6)
    private BigDecimal annualDelta;
    @Column(name = "calculation_basis", length = 2000)
    private String calculationBasis;
    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
