package com.automotive.costing.costingscenario.dto;

import com.automotive.costing.costingscenario.entity.AdditiveCostType;
import com.automotive.costing.costingscenario.entity.ScenarioStatus;
import com.automotive.costing.costingscenario.entity.ScenarioType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CostingScenarioDtos {
    private CostingScenarioDtos() {}

    public record ScenarioResponse(
            String scenarioId,
            String bomNodeId,
            String programId,
            String scenarioCode,
            String scenarioName,
            ScenarioType scenarioType,
            ScenarioStatus status,
            String parentScenarioId,
            String shouldCostBuildUpId,
            String costingVariantId,
            String notes,
            boolean locked,
            BigDecimal additiveTotal,
            BigDecimal additivePerUnitTotal,
            int additiveLineCount
    ) {}

    public record AdditiveLineResponse(
            String lineId,
            String lineCode,
            String lineName,
            AdditiveCostType additiveType,
            BigDecimal amount,
            String currencyCode,
            boolean amortized,
            BigDecimal amortizationVolume,
            BigDecimal amountPerUnit,
            String notes
    ) {}

    public record ScenarioDetailResponse(ScenarioResponse scenario, List<AdditiveLineResponse> additiveLines) {}

    public record CreateScenarioRequest(
            @NotBlank String bomNodeId,
            String programId,
            @NotBlank String scenarioCode,
            @NotBlank String scenarioName,
            @NotNull ScenarioType scenarioType,
            String parentScenarioId,
            String shouldCostBuildUpId,
            String costingVariantId,
            String notes
    ) {}

    public record BranchScenarioRequest(
            @NotBlank String sourceScenarioId,
            @NotBlank String newScenarioCode,
            @NotBlank String newScenarioName,
            @NotNull ScenarioType scenarioType,
            String notes
    ) {}

    public record AddAdditiveLineRequest(
            @NotBlank String lineCode,
            @NotBlank String lineName,
            @NotNull AdditiveCostType additiveType,
            @NotNull BigDecimal amount,
            String currencyCode,
            Boolean amortized,
            BigDecimal amortizationVolume,
            String notes
    ) {}

    public record ScenarioCompareResponse(
            String bomNodeId,
            List<ScenarioResponse> scenarios,
            String note
    ) {}
}
