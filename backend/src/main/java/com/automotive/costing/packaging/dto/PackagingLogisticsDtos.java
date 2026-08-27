package com.automotive.costing.packaging.dto;

import com.automotive.costing.packaging.entity.FreightMode;
import com.automotive.costing.packaging.entity.PackagingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PackagingLogisticsDtos {

    private PackagingLogisticsDtos() {
    }

    public record CreateProfileRequest(
            @NotBlank String bomNodeId,
            String profileLabel,
            @NotNull PackagingType packagingType,
            @NotNull @DecimalMin("0.0") BigDecimal primaryPackCostPerPiece,
            @NotNull @DecimalMin("0.0") BigDecimal secondaryPackCostPerPiece,
            @DecimalMin("0.0") BigDecimal returnableContainerCost,
            @DecimalMin("0.0") BigDecimal containerLifeCycles,
            @DecimalMin("0.0") BigDecimal netWeightKg,
            @NotNull FreightMode freightMode,
            @DecimalMin("0.0") BigDecimal freightRatePerKg,
            @DecimalMin("0.0") BigDecimal fixedFreightPerUnit,
            @DecimalMin("0.0") BigDecimal dutyPct,
            String originPlantId,
            String destinationFreightZone,
            String notes
    ) {
    }

    public record UpdateProfileRequest(
            String profileLabel,
            PackagingType packagingType,
            @DecimalMin("0.0") BigDecimal primaryPackCostPerPiece,
            @DecimalMin("0.0") BigDecimal secondaryPackCostPerPiece,
            @DecimalMin("0.0") BigDecimal returnableContainerCost,
            @DecimalMin("0.0") BigDecimal containerLifeCycles,
            @DecimalMin("0.0") BigDecimal netWeightKg,
            FreightMode freightMode,
            @DecimalMin("0.0") BigDecimal freightRatePerKg,
            @DecimalMin("0.0") BigDecimal fixedFreightPerUnit,
            @DecimalMin("0.0") BigDecimal dutyPct,
            String originPlantId,
            String destinationFreightZone,
            String notes,
            Boolean active
    ) {
    }

    public record ProfileResponse(
            String profileId,
            String bomNodeId,
            String profileLabel,
            PackagingType packagingType,
            BigDecimal primaryPackCostPerPiece,
            BigDecimal secondaryPackCostPerPiece,
            BigDecimal returnableContainerCost,
            BigDecimal containerLifeCycles,
            BigDecimal netWeightKg,
            FreightMode freightMode,
            BigDecimal freightRatePerKg,
            BigDecimal fixedFreightPerUnit,
            BigDecimal dutyPct,
            String originPlantId,
            String destinationFreightZone,
            String notes,
            boolean active
    ) {
    }

    public record CalculateRequest(
            @NotBlank String profileId
    ) {
    }

    public record CalculationResponse(
            String calculationId,
            String profileId,
            String bomNodeId,
            String programId,
            BigDecimal packagingCostPerPiece,
            BigDecimal freightCostPerPiece,
            BigDecimal dutyCostPerPiece,
            BigDecimal totalPackagingFreightCost,
            String calculationBasis,
            Instant calculatedAt
    ) {
    }

    public record CompareRequest(
            List<String> profileIds
    ) {
    }

    public record CompareLine(
            String profileId,
            String profileLabel,
            PackagingType packagingType,
            BigDecimal totalPackagingFreightCost
    ) {
    }

    public record CompareResponse(
            List<CompareLine> lines,
            String lowestCostProfileId
    ) {
    }
}
