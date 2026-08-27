package com.automotive.costing.coststructure.entity;

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
@Table(name = "cost_estimate_item")
public class CostEstimateItem {

    @Id
    @Column(name = "item_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String itemId;

    @Column(name = "estimate_ref_type", length = 40, nullable = false)
    private String estimateRefType;

    @Column(name = "estimate_ref_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String estimateRefId;

    @Column(name = "bom_node_id", length = 36, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "component_code", length = 40, nullable = false)
    private String componentCode;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "quantity", precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_of_measure", length = 30)
    private String unitOfMeasure;

    @Column(name = "rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Column(name = "cost_behavior", length = 20, nullable = false)
    private String costBehavior = "VARIABLE";

    @Column(name = "price_source", length = 80)
    private String priceSource;

    @Column(name = "calculation_rule", length = 500)
    private String calculationRule;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
