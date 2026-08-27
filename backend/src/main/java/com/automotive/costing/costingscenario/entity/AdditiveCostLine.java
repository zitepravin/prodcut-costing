package com.automotive.costing.costingscenario.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "additive_cost_line")
public class AdditiveCostLine {

    @Id
    @Column(name = "line_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String lineId;

    @Column(name = "scenario_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String scenarioId;

    @Column(name = "line_code", length = 40, nullable = false)
    private String lineCode;

    @Column(name = "line_name", length = 200, nullable = false)
    private String lineName;

    @Enumerated(EnumType.STRING)
    @Column(name = "additive_type", length = 40, nullable = false)
    private AdditiveCostType additiveType;

    @Column(name = "amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Column(name = "amortized", nullable = false)
    private boolean amortized;

    @Column(name = "amortization_volume", precision = 18, scale = 6)
    private BigDecimal amortizationVolume;

    @Column(name = "amount_per_unit", precision = 18, scale = 6)
    private BigDecimal amountPerUnit;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
