package com.automotive.costing.mfgintel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class MfgIntelDtos {
    private MfgIntelDtos() {}

    public record ActivityTypeResponse(
            String activityTypeId, String activityCode, String activityName, String category,
            String unitOfMeasure, BigDecimal defaultRate, String currencyCode,
            String costCenterCode, boolean active, String notes
    ) {}

    public record CreateActivityTypeRequest(
            @NotBlank String activityCode,
            @NotBlank String activityName,
            String category,
            String unitOfMeasure,
            BigDecimal defaultRate,
            String currencyCode,
            String costCenterCode,
            String notes
    ) {}

    public record ActivityCostRequest(
            @NotBlank String activityCode,
            @NotNull BigDecimal quantity,
            BigDecimal rateOverride
    ) {}

    public record ActivityCostResponse(
            String activityCode, String activityName, String category,
            BigDecimal quantity, String unitOfMeasure, BigDecimal rateUsed,
            BigDecimal activityCost, String currencyCode, String calculationBasis
    ) {}

    public record CapacityCenterResponse(
            String capacityCenterId, String centerCode, String centerName, String plantCode,
            BigDecimal availableHours, String periodCode, BigDecimal efficiencyPct, boolean active
    ) {}

    public record CreateCapacityCenterRequest(
            @NotBlank String centerCode,
            @NotBlank String centerName,
            String plantCode,
            @NotNull BigDecimal availableHours,
            @NotBlank String periodCode,
            BigDecimal efficiencyPct,
            String notes
    ) {}

    public record AddLoadRequest(
            @NotNull BigDecimal requiredHours,
            String activityTypeId,
            String sourceRefType,
            String sourceRefId,
            String loadLabel
    ) {}

    public record LoadResponse(
            String loadId, BigDecimal requiredHours, String activityTypeId,
            String sourceRefType, String sourceRefId, String loadLabel
    ) {}

    public record UtilizationResponse(
            String capacityCenterId, String centerCode, String periodCode,
            BigDecimal availableHours, BigDecimal efficiencyPct, BigDecimal effectiveHours,
            BigDecimal requiredHours, BigDecimal residualHours, BigDecimal utilizationPct,
            String status, List<LoadResponse> loads
    ) {}
}
