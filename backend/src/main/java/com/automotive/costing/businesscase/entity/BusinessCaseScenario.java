package com.automotive.costing.businesscase.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
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
@Table(name = "business_case_scenario")
public class BusinessCaseScenario extends BaseAuditableEntity {

    @Id
    @Column(name = "scenario_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String scenarioId;

    @Column(name = "program_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "scenario_code", length = 40, nullable = false)
    private String scenarioCode;

    @Column(name = "scenario_name", length = 200, nullable = false)
    private String scenarioName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BusinessCaseStatus status = BusinessCaseStatus.DRAFT;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Column(name = "unit_selling_price", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitSellingPrice = BigDecimal.ZERO;

    @Column(name = "unit_should_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitShouldCost = BigDecimal.ZERO;

    @Column(name = "unit_variable_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitVariableCost = BigDecimal.ZERO;

    @Column(name = "annual_volume", precision = 18, scale = 6, nullable = false)
    private BigDecimal annualVolume = BigDecimal.ZERO;

    @Column(name = "horizon_years", nullable = false)
    private int horizonYears = 5;

    @Column(name = "discount_rate_pct", precision = 9, scale = 6, nullable = false)
    private BigDecimal discountRatePct = new BigDecimal("0.100000");

    @Column(name = "initial_investment", precision = 18, scale = 6, nullable = false)
    private BigDecimal initialInvestment = BigDecimal.ZERO;

    @Column(name = "fixed_annual_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal fixedAnnualCost = BigDecimal.ZERO;

    @Column(name = "contribution_margin_unit", precision = 18, scale = 6)
    private BigDecimal contributionMarginUnit;

    @Column(name = "annual_contribution", precision = 18, scale = 6)
    private BigDecimal annualContribution;

    @Column(name = "npv_amount", precision = 18, scale = 6)
    private BigDecimal npvAmount;

    @Column(name = "payback_years", precision = 18, scale = 6)
    private BigDecimal paybackYears;

    @Column(name = "calculation_basis", length = 2000)
    private String calculationBasis;

    @Column(name = "calculated_at")
    private Instant calculatedAt;
}
