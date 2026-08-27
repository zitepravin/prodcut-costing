package com.automotive.costing.costingsheet.entity;

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
@Table(name = "costing_sheet_rule")
public class CostingSheetRule {

    @Id
    @Column(name = "rule_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String ruleId;

    @Column(name = "sheet_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String sheetId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "rule_code", length = 40, nullable = false)
    private String ruleCode;

    @Column(name = "rule_name", length = 200, nullable = false)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", length = 30, nullable = false)
    private CostingSheetRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_code", length = 40)
    private CostingSheetBaseCode baseCode;

    @Column(name = "percent_rate", precision = 18, scale = 8)
    private BigDecimal percentRate;

    @Column(name = "fixed_amount", precision = 18, scale = 6)
    private BigDecimal fixedAmount;

    @Column(name = "component_code", length = 40, nullable = false)
    private String componentCode;

    @Column(name = "cost_behavior", length = 20, nullable = false)
    private String costBehavior = "FIXED";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
