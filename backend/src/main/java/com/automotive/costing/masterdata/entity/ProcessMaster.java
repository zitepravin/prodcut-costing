package com.automotive.costing.masterdata.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Standard process library (stamping, forging, casting, etc.) with default scrap%. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "process_master")
public class ProcessMaster {

    @Id
    @Column(name = "process_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String processId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "process_name", length = 100, nullable = false, unique = true)
    private String processName;

    @Column(name = "default_cycle_time_formula", columnDefinition = "TEXT")
    private String defaultCycleTimeFormula;

    @Column(name = "default_scrap_pct", precision = 9, scale = 6)
    private BigDecimal defaultScrapPct;
}
