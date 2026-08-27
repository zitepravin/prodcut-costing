package com.automotive.costing.currency.entity;

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
 * Append-only FX rate row (M12). A new row is published for every rate change;
 * never update rate or effective_date in place (same pattern as commodity_index_value).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exchange_rate")
public class ExchangeRate extends BaseAuditableEntity {

    @Id
    @Column(name = "rate_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String rateId;

    @Column(name = "from_currency", length = 3, nullable = false, updatable = false, columnDefinition = "CHAR(3)")
    private String fromCurrency;

    @Column(name = "to_currency", length = 3, nullable = false, updatable = false, columnDefinition = "CHAR(3)")
    private String toCurrency;

    /** Multiplier: amount_in_to = amount_in_from * rate */
    @Column(name = "rate", precision = 18, scale = 8, nullable = false, updatable = false)
    private BigDecimal rate;

    @Column(name = "effective_date", nullable = false, updatable = false)
    private LocalDate effectiveDate;

    @Column(name = "source", length = 100)
    private String source;
}
