package com.automotive.costing.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Minimal Currency reference needed by M01/M05; full M12 Currency & Duty module is Phase 2. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "currency")
public class Currency {

    @Id
    @Column(name = "currency_code", length = 3, nullable = false, updatable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "description", length = 50)
    private String description;

    @Column(name = "decimal_places")
    private Integer decimalPlaces;
}
