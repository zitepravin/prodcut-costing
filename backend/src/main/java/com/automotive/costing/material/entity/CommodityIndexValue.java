package com.automotive.costing.material.entity;

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

/**
 * A single dated publication of a commodity index (e.g. LME aluminum, HRC steel index) -
 * MAT-02: "index-linked pricing formulas referencing an external or manually updated
 * commodity index value." Append-only, mirroring the MDM-02 versioning convention used
 * across the rest of master data: a new index value is a new row, never an in-place edit
 * of a prior value, so historical should-cost calculations remain reproducible (TDD
 * Section 6.2 idempotency requirement) even after the index moves.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "commodity_index_value")
public class CommodityIndexValue extends BaseAuditableEntity {

    @Id
    @Column(name = "index_value_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String indexValueId;

    @Column(name = "index_code", length = 30, nullable = false)
    private String indexCode;

    @Column(name = "index_value", precision = 18, scale = 6, nullable = false)
    private BigDecimal indexValue;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "source", length = 100)
    private String source;
}
