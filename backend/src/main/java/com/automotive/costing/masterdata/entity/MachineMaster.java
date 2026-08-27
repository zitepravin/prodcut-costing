package com.automotive.costing.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Machine master (feeds M04 Process/Labor Costing and M05 rate build-up, OHD-01).
 * `rate` on the parent class holds the fully composed hourly rate; the components
 * below are retained individually so OHD-01's "each independently editable" and
 * OHD-06's what-if simulation can recompute the total without re-deriving history.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "machine_master")
public class MachineMaster extends AbstractMasterRateRecord {

    @Column(name = "machine_type", length = 100)
    private String machineType;

    @Column(name = "capacity", length = 100)
    private String capacity;

    @Column(name = "depreciation_life_years", precision = 9, scale = 2)
    private BigDecimal depreciationLifeYears;

    @Column(name = "capital_cost", precision = 18, scale = 6)
    private BigDecimal capitalCost;

    @Column(name = "annual_operating_hours", precision = 18, scale = 2)
    private BigDecimal annualOperatingHours;

    @Column(name = "power_cost_per_hour", precision = 18, scale = 6)
    private BigDecimal powerCostPerHour;

    @Column(name = "maintenance_cost_per_hour", precision = 18, scale = 6)
    private BigDecimal maintenanceCostPerHour;

    @Column(name = "floor_space_cost_per_hour", precision = 18, scale = 6)
    private BigDecimal floorSpaceCostPerHour;

    @Column(name = "shift_pattern", length = 20)
    private String shiftPattern; // 1-shift / 2-shift / 3-shift (OHD-04)
}
