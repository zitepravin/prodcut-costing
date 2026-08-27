package com.automotive.costing.benchmark.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "benchmark_rate")
public class BenchmarkRate extends BaseAuditableEntity {

    @Id
    @Column(name = "rate_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String rateId;

    @Column(name = "pack_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String packId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30, nullable = false)
    private BenchmarkCategory category;

    @Column(name = "process_family", length = 80)
    private String processFamily;

    @Column(name = "rate_code", length = 40, nullable = false)
    private String rateCode;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "rate_value", precision = 18, scale = 6, nullable = false)
    private BigDecimal rateValue;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Column(name = "unit_of_measure", length = 30, nullable = false)
    private String unitOfMeasure;

    @Column(name = "confidence", length = 20, nullable = false)
    private String confidence = "MEDIUM";
}
