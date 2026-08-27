package com.automotive.costing.targetcosting.dto;

import com.automotive.costing.targetcosting.entity.ActualCostSource;
import com.automotive.costing.targetcosting.entity.TargetApprovalStatus;
import com.automotive.costing.targetcosting.entity.TargetCostStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class TargetCostingDtos {

    private TargetCostingDtos() {
    }

    public record CreateTargetRequest(
            @NotBlank String bomNodeId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal targetAmount,
            String currencyCode,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes
    ) {
    }

    public record UpdateTargetRequest(
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal targetAmount,
            String currencyCode,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes
    ) {
    }

    public record TargetResponse(
            String targetCostId,
            String bomNodeId,
            String programId,
            BigDecimal targetAmount,
            String currencyCode,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            TargetCostStatus status,
            TargetApprovalStatus approvalStatus,
            String notes
    ) {
    }

    public record ComputeVarianceRequest(
            @NotBlank String targetCostId,
            @NotNull ActualCostSource actualSource,
            /** Required when actualSource = SHOULD_COST; ignored for BOM_ROLLUP (uses node rolledUpCost). */
            String shouldCostBuildUpId,
            /** Fraction [0,1); default 0.05 (5%) when null. */
            BigDecimal thresholdPct
    ) {
    }

    public record VarianceResponse(
            String varianceId,
            String targetCostId,
            String bomNodeId,
            String programId,
            BigDecimal targetAmount,
            BigDecimal actualAmount,
            ActualCostSource actualSource,
            String actualSourceId,
            BigDecimal absoluteVariance,
            BigDecimal percentageVariance,
            boolean exceedsThreshold,
            BigDecimal thresholdPct,
            String calculationBasis,
            Instant calculatedAt
    ) {
    }

    public record ActivateRequest(
            // empty — path param only; reserved for future justification field
    ) {
    }

    public record ExceedingVarianceSummary(
            List<VarianceResponse> items,
            long totalExceeding
    ) {
    }
}
