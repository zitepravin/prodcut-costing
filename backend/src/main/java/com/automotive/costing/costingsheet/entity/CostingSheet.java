package com.automotive.costing.costingsheet.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "costing_sheet")
public class CostingSheet extends BaseAuditableEntity {

    @Id
    @Column(name = "sheet_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String sheetId;

    @Column(name = "sheet_code", length = 40, nullable = false)
    private String sheetCode;

    @Column(name = "sheet_name", length = 200, nullable = false)
    private String sheetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CostingSheetStatus status = CostingSheetStatus.DRAFT;

    @Column(name = "plant_code", length = 40)
    private String plantCode;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "description", length = 1000)
    private String description;
}
