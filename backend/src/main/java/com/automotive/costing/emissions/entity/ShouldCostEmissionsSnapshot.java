package com.automotive.costing.emissions.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "should_cost_emissions_snapshot")
public class ShouldCostEmissionsSnapshot {
    @Id
    @Column(name = "snapshot_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String snapshotId;
    @Column(name = "build_up_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String buildUpId;
    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;
    @Column(name = "material_co2e_kg", precision = 18, scale = 6)
    private BigDecimal materialCo2eKg;
    @Column(name = "process_co2e_kg", precision = 18, scale = 6)
    private BigDecimal processCo2eKg;
    @Column(name = "total_co2e_kg", precision = 18, scale = 6)
    private BigDecimal totalCo2eKg;
    @Column(name = "emissions_status", length = 20, nullable = false)
    private String emissionsStatus = "NOT_AVAILABLE";
    @Column(name = "factor_id", length = 36, columnDefinition = "CHAR(36)")
    private String factorId;
    @Column(name = "factor_code", length = 50)
    private String factorCode;
    @Column(name = "factor_version_no")
    private Integer factorVersionNo;
    @Column(name = "quantity_kg", precision = 18, scale = 6)
    private BigDecimal quantityKg;
    @Column(name = "calculation_notes", length = 2000)
    private String calculationNotes;
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
