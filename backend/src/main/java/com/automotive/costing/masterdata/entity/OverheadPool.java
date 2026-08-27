package com.automotive.costing.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Overhead pool (OHD-02: at least 4 configurable pools with independent allocation bases).
 * `rate` on the parent class holds the absorption rate (pool total / allocation base).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "overhead_pool")
public class OverheadPool extends AbstractMasterRateRecord {

    @Column(name = "pool_category", length = 50)
    private String poolCategory; // Factory Overhead / Corporate SG&A / Quality / Logistics Overhead

    @Column(name = "allocation_base", length = 100)
    private String allocationBase; // direct labor hours / machine hours / material cost

    @Column(name = "period", length = 20)
    private String period; // e.g. "2026-Q3", "2026" for annual pools
}
