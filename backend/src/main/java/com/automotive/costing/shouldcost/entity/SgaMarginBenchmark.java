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
 * SRS 4.8.5: "SG&amp;A and profit margin benchmarks applied must be configurable by
 * commodity family and region, not a single global constant." A simpler reference-data
 * shape than {@code MaterialMaster}/{@code MachineMaster} deliberately - no MDM-02-style
 * version chain, just an active/inactive flag - since SG&amp;A/margin benchmarks change
 * far less frequently and don't carry the same financial-calculation-correctness stakes
 * as a material or machine rate (this is a documented simplification, flagged in
 * NEXT_AGENT.md as a candidate for the full versioning pattern if the client's actual
 * benchmark-revision cadence turns out to need it).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sga_margin_benchmark")
public class SgaMarginBenchmark extends BaseAuditableEntity {

    @Id
    @Column(name = "benchmark_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String benchmarkId;

    @Column(name = "commodity_family", length = 100, nullable = false)
    private String commodityFamily;

    @Column(name = "region", length = 50, nullable = false)
    private String region;

    @Column(name = "sga_pct", precision = 9, scale = 6, nullable = false)
    private BigDecimal sgaPct;

    @Column(name = "margin_pct", precision = 9, scale = 6, nullable = false)
    private BigDecimal marginPct;

    @Column(name = "active", nullable = false)
    private boolean active;
}
