package com.automotive.costing.emissions.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "emission_factor")
public class EmissionFactor extends BaseAuditableEntity {
    @Id
    @Column(name = "factor_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String factorId;
    @Column(name = "factor_code", length = 50, nullable = false)
    private String factorCode;
    @Column(name = "factor_name", length = 200, nullable = false)
    private String factorName;
    @Column(name = "category", length = 20, nullable = false)
    private String category = "MATERIAL";
    @Column(name = "co2e_value", precision = 18, scale = 6, nullable = false)
    private BigDecimal co2eValue;
    @Column(name = "unit", length = 40, nullable = false)
    private String unit = "KG_CO2E_PER_KG";
    @Column(name = "source_type", length = 30, nullable = false)
    private String sourceType = "MANUAL";
    @Column(name = "source_reference", length = 500)
    private String sourceReference;
    @Column(name = "geography", length = 40)
    private String geography;
    @Column(name = "version_no", nullable = false)
    private int versionNo = 1;
    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
