package com.automotive.costing.tooling.dto;

import com.automotive.costing.tooling.entity.ToolingAssetStatus;
import com.automotive.costing.tooling.entity.ToolingOwnership;
import com.automotive.costing.tooling.entity.ToolingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ToolingRecordDtos {

    public record CreateRequest(
            @NotBlank(message = "bomNodeId is required") String bomNodeId,
            @NotNull(message = "toolingType is required") ToolingType toolingType,
            String description,
            Integer cavityCount,
            @NotNull(message = "toolCost is required")
            @DecimalMin(value = "0.0", inclusive = false, message = "toolCost must be > 0")
            BigDecimal toolCost,
            BigDecimal toolLifeHits,
            BigDecimal refurbishmentCostPerEvent,
            BigDecimal cumulativeVolumeToDate,
            @NotNull(message = "ownership is required") ToolingOwnership ownership,
            String supplierId,
            String toolingQuoteReference,
            String plantLocation,
            LocalDate installationDate
    ) {
    }

    public record Response(
            String toolingRecordId,
            String bomNodeId,
            ToolingType toolingType,
            String description,
            Integer cavityCount,
            BigDecimal toolCost,
            BigDecimal toolLifeHits,
            BigDecimal refurbishmentCostPerEvent,
            BigDecimal cumulativeVolumeToDate,
            ToolingOwnership ownership,
            String supplierId,
            String toolingQuoteReference,
            String plantLocation,
            LocalDate installationDate,
            ToolingAssetStatus assetStatus
    ) {
    }

    /** TL-07: retiring a tool keeps it in the asset register (never deleted), just excluded from active use. */
    public record RetireRequest(String reason) {
    }
}
