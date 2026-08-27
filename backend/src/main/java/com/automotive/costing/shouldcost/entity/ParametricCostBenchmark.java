package com.automotive.costing.shouldcost.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * SC-02: "a rapid parametric should-cost mode requiring only part weight, material
 * family, and process family as inputs." This table is the $/kg benchmark that mode reads
 * from - deliberately separate from {@code MaterialMaster}/{@code ProcessMaster} (which
 * are per-grade/per-process reference data, not weight-based cost benchmarks) and
 * deliberately NOT wired into {@code MaterialCostEngine}/{@code ProcessCostEngine} - the
 * whole point of the parametric mode is to bypass the full bottom-up calculation for
 * early concept-stage estimation before a real BOM/routing exists.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "parametric_cost_benchmark")
public class ParametricCostBenchmark extends BaseAuditableEntity {

    @Id
    @Column(name = "benchmark_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String benchmarkId;

    @Column(name = "material_family", length = 100, nullable = false)
    private String materialFamily;

    @Column(name = "process_family", length = 100, nullable = false)
    private String processFamily;

    @Column(name = "region", length = 50, nullable = false)
    private String region;

    @Column(name = "cost_per_kg", precision = 18, scale = 6, nullable = false)
    private BigDecimal costPerKg;

    @Column(name = "active", nullable = false)
    private boolean active;
}
