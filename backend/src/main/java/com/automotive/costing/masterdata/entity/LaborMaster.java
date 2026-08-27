package com.automotive.costing.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Labor master (feeds M04 Process/Labor Costing). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "labor_master")
public class LaborMaster extends AbstractMasterRateRecord {

    @Column(name = "operator_grade", length = 50)
    private String operatorGrade;

    @Column(name = "shift_type", length = 20)
    private String shiftType;

    @Column(name = "country_region", length = 50)
    private String countryRegion;

    @Column(name = "statutory_burden_pct", precision = 9, scale = 6)
    private BigDecimal statutoryBurdenPct;
}
