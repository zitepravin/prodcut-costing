package com.automotive.costing.shouldcost.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ShouldCostDtos {

    /** SC-01: full bottom-up build-up for a BOM node that already has material/process/tooling calculated. */
    public record GenerateRequest(
            @NotBlank(message = "bomNodeId is required") String bomNodeId,
            String scenarioLabel, // SC-06 - defaults to "Current Design" if omitted
            @NotBlank(message = "commodityFamily is required") String commodityFamily,
            String region, // omitted = falls back to the owning VehicleProgram.sourcingRegion
            BigDecimal allocationBaseQuantity, // overhead allocation base (e.g. total routing labor hours); 0/omitted = no overhead
            String overheadPoolCode,
            /** Optional A1 costing variant — when set, ACTIVE variant supplies OH pool default and basis metadata. */
            String costingVariantId,
            /** Costing/valuation date for FX. Null = generate-time boundary only. */
            java.time.LocalDate costingDate
    ) {
    }

    public record Response(
            String buildUpId,
            String bomNodeId,
            String scenarioLabel,
            BigDecimal materialCost,
            BigDecimal processCost,
            BigDecimal toolingCost,
            BigDecimal packagingFreightCost,
            BigDecimal overheadCost,
            BigDecimal sgaCost,
            BigDecimal profitMargin,
            BigDecimal totalCost,
            boolean staleInputsFlag,
            boolean locked,
            String approvalStatus,
            String calculationBasis,
            Instant calculatedAt
    ) {
    }

    /** SC-02: rapid parametric mode - weight + material family + process family only. */
    public record ParametricRequest(
            @NotBlank(message = "materialFamily is required") String materialFamily,
            @NotBlank(message = "processFamily is required") String processFamily,
            @NotBlank(message = "region is required") String region,
            @DecimalMin(value = "0.0", inclusive = false, message = "netWeightKg must be > 0")
            BigDecimal netWeightKg
    ) {
    }

    public record ParametricResponse(
            BigDecimal netWeightKg,
            BigDecimal costPerKg,
            BigDecimal estimatedCost
    ) {
    }

    /** SC-04: percentage/absolute variance between should-cost and a linked (or ad-hoc) supplier quotation. */
    public record VarianceRequest(
            @NotBlank(message = "buildUpId is required") String buildUpId,
            BigDecimal quotedPrice
    ) {
    }

    public record VarianceResponse(
            BigDecimal shouldCost,
            BigDecimal quotedPrice,
            BigDecimal absoluteVariance,
            BigDecimal percentageVariance
    ) {
    }

    /** SC-03: cost breakdown waterfall - the same Response fields, ordered for chart consumption. */
    public record WaterfallPoint(String element, BigDecimal amount) {
    }

    public record WaterfallResponse(String buildUpId, List<WaterfallPoint> elements) {
    }

    /** SC-05: multiple regional variants of the same part side by side. */
    public record RegionalCompareRequest(List<String> buildUpIds) {
    }

    public record RegionalCompareLine(String buildUpId, String scenarioLabel, BigDecimal totalCost) {
    }

    public record RegionalCompareResponse(List<RegionalCompareLine> lines, String lowestCostBuildUpId) {
    }

    public record QuoteComponentSplit(@NotBlank String componentCode, @DecimalMin("0.0") BigDecimal quotedAmount) {}
    public record DeepVarianceRequest(@NotBlank String buildUpId, @DecimalMin("0.0") BigDecimal quotedTotal, List<QuoteComponentSplit> componentSplits) {}
    public record ComponentAmountDto(String componentCode, String displayName, int sequenceNo, BigDecimal shouldCostAmount,
        BigDecimal quotedAmount, BigDecimal absoluteVariance, BigDecimal percentageVariance, String dataStatus) {}
    public record DeepVarianceResponse(String buildUpId, BigDecimal shouldCostTotal, BigDecimal quotedTotal,
        BigDecimal absoluteVarianceTotal, BigDecimal percentageVarianceTotal, List<ComponentAmountDto> components,
        List<String> topContributors, String explanation) {}
    public record CostComponentDefinitionDto(String componentId, String componentCode, String displayName,
        int sequenceNo, String category, String mapsToColumn, boolean active) {}

}
