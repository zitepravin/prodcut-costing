package com.automotive.costing.lifecycle.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class LifecycleDtos {

    private LifecycleDtos() {
    }

    public record YearInput(
            @NotNull Integer yearIndex,
            Integer calendarYear,
            @NotNull @DecimalMin("0.0") BigDecimal plannedVolume
    ) {
    }

    public record UpsertCurveRequest(
            String scenarioLabel,
            String notes,
            @NotEmpty @Valid List<YearInput> years
    ) {
    }

    public record YearResponse(
            int yearIndex,
            Integer calendarYear,
            BigDecimal plannedVolume
    ) {
    }

    public record CurveResponse(
            String curveId,
            String programId,
            String scenarioCode,
            String scenarioLabel,
            String status,
            String notes,
            List<YearResponse> years
    ) {
    }

    public record YearProjectionResponse(
            int yearIndex,
            Integer calendarYear,
            BigDecimal plannedVolume,
            BigDecimal unitCost,
            BigDecimal yearTotalCost,
            BigDecimal cumulativeVolume,
            BigDecimal cumulativeCost
    ) {
    }

    public record LifecycleProjectionResponse(
            String programId,
            String scenarioCode,
            String curveId,
            String reportingCurrency,
            String unitCostBaselineStatus,
            BigDecimal unitCostBaseline,
            BigDecimal volumeInsensitiveUnit,
            BigDecimal toolingPerUnit,
            String toolingStatus,
            BigDecimal lifetimeVolumeForCurve,
            List<YearProjectionResponse> years,
            List<String> dataQualityNotes
    ) {
    }
}
