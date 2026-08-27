package com.automotive.costing.standardcost.dto;

import com.automotive.costing.standardcost.entity.StandardCostStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class StandardCostDtos {
    private StandardCostDtos() {}

    public record StandardCostResponse(
            String standardCostId,
            String bomNodeId,
            String programId,
            int versionNo,
            StandardCostStatus status,
            String sourceType,
            String sourceRefId,
            BigDecimal unitCost,
            String currencyCode,
            BigDecimal materialCost,
            BigDecimal processCost,
            BigDecimal toolingCost,
            BigDecimal packagingCost,
            BigDecimal overheadCost,
            BigDecimal sgaCost,
            BigDecimal profitMargin,
            String costingVariantId,
            String plantCode,
            LocalDate validFrom,
            LocalDate validTo,
            Instant releasedAt,
            String releasedBy,
            Instant frozenAt,
            String frozenBy,
            String supersededById,
            String calculationBasis,
            String notes
    ) {}

    public record CreateFromShouldCostRequest(
            @NotBlank String shouldCostBuildUpId,
            String plantCode,
            LocalDate validFrom,
            LocalDate validTo,
            String notes
    ) {}

    public record CreateManualRequest(
            @NotBlank String bomNodeId,
            String programId,
            @NotBlank BigDecimal unitCost,
            String currencyCode,
            BigDecimal materialCost,
            BigDecimal processCost,
            BigDecimal toolingCost,
            BigDecimal packagingCost,
            BigDecimal overheadCost,
            String plantCode,
            LocalDate validFrom,
            LocalDate validTo,
            String notes
    ) {}

    public record LifecycleHistoryResponse(
            String bomNodeId,
            List<StandardCostResponse> versions,
            StandardCostResponse currentReleased
    ) {}
}
