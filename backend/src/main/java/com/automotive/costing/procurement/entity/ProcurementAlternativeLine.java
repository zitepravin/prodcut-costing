package com.automotive.costing.procurement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "procurement_alternative_line")
public class ProcurementAlternativeLine {

    @Id
    @Column(name = "line_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String lineId;

    @Column(name = "set_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String setId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "alternative_type", length = 40, nullable = false)
    private AlternativeType alternativeType;

    @Column(name = "source_ref_type", length = 40)
    private String sourceRefType;

    @Column(name = "source_ref_id", length = 36, columnDefinition = "CHAR(36)")
    private String sourceRefId;

    @Column(name = "supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "unit_cost", precision = 18, scale = 6)
    private BigDecimal unitCost;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "tco_unit", precision = 18, scale = 6)
    private BigDecimal tcoUnit;

    @Column(name = "tco_status", length = 20, nullable = false)
    private String tcoStatus = "DATA_NOT_AVAILABLE";

    @Column(name = "eligibility", length = 20, nullable = false)
    private String eligibility = "UNKNOWN";

    @Column(name = "rank_no")
    private Integer rankNo;

    @Column(name = "data_status", length = 30, nullable = false)
    private String dataStatus = "OK";

    @Column(name = "notes", length = 500)
    private String notes;
}
