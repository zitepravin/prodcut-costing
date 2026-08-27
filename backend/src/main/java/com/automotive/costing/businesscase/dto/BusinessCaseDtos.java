package com.automotive.costing.businesscase.dto;

import com.automotive.costing.businesscase.entity.BusinessCaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public final class BusinessCaseDtos {
    private BusinessCaseDtos() {}

    public record ScenarioResponse(
            String scenarioId,
            String programId,
            String scenarioCode,
            String scenarioName,
            BusinessCaseStatus status,
            String currencyCode,
            BigDecimal unitSellingPrice,
            BigDecimal unitShouldCost,
            BigDecimal unitVariableCost,
            BigDecimal annualVolume,
            int horizonYears,
            BigDecimal discountRatePct,
            BigDecimal initialInvestment,
            BigDecimal fixedAnnualCost,
            BigDecimal contributionMarginUnit,
            BigDecimal annualContribution,
            BigDecimal npvAmount,
            BigDecimal paybackYears,
            String calculationBasis,
            Instant calculatedAt
    ) {}

    public record CreateScenarioRequest(
            @NotBlank String programId,
            @NotBlank String scenarioCode,
            @NotBlank String scenarioName,
            String currencyCode,
            @NotNull BigDecimal unitSellingPrice,
            @NotNull BigDecimal unitShouldCost,
            BigDecimal unitVariableCost,
            @NotNull BigDecimal annualVolume,
            Integer horizonYears,
            BigDecimal discountRatePct,
            BigDecimal initialInvestment,
            BigDecimal fixedAnnualCost
    ) {}
}
