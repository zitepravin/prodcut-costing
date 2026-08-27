package com.automotive.costing.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Material master (SRS Section 4.1.3/4.1.4 MDM-*; feeds M03 Raw Material Cost Management).
 * grade/specification/density/scrapRecoveryRate are material-specific extensions
 * beyond the shared AbstractMasterRateRecord columns.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "material_master")
public class MaterialMaster extends AbstractMasterRateRecord {

    @Column(name = "grade", length = 50)
    private String grade;

    @Column(name = "specification_standard", length = 50)
    private String specificationStandard; // JIS, DIN, ASTM, SAE

    @Column(name = "density", precision = 18, scale = 6)
    private BigDecimal density;

    @Column(name = "scrap_recovery_rate", precision = 18, scale = 6)
    private BigDecimal scrapRecoveryRate; // MAT-03: scrap resale credit basis

    @Column(name = "material_form", length = 30)
    private String materialForm; // sheet/coil/bar/billet/casting/forging_blank/resin_pellet/rubber_compound (MAT-06)

    // --- MAT-02 index-linked pricing (V4__material_costing_module.sql) ---

    @Column(name = "index_linked_flag", nullable = false)
    private boolean indexLinkedFlag;

    @Column(name = "index_code", length = 30)
    private String indexCode; // references commodity_index_value.index_code

    @Column(name = "base_price", precision = 18, scale = 6)
    private BigDecimal basePrice;

    @Column(name = "alloy_surcharge_per_unit", precision = 18, scale = 6)
    private BigDecimal alloySurchargePerUnit;

    /** P3-11a: optional default material emission factor (ACTIVE factors only for calculation). */
    @Column(name = "default_emission_factor_id", length = 36, columnDefinition = "CHAR(36)")
    private String defaultEmissionFactorId;
}
