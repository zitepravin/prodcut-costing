package com.automotive.costing.masterdata.dto;

import com.automotive.costing.masterdata.entity.MasterDataStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class OverheadPoolDtos {

    public record CreateRequest(
            @NotBlank(message = "code is required") String code,
            String description,
            @NotBlank(message = "poolCategory is required") String poolCategory,
            @NotBlank(message = "allocationBase is required") String allocationBase,
            @NotNull(message = "poolTotal is required") @DecimalMin(value = "0.0", message = "poolTotal must be >= 0")
            BigDecimal poolTotal,
            @NotNull(message = "allocationBaseVolume is required")
            @DecimalMin(value = "0.0", inclusive = false, message = "allocationBaseVolume must be > 0")
            BigDecimal allocationBaseVolume,
            String uom,
            String plantId,
            @NotNull(message = "effectiveFrom is required") LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String period
    ) {
    }

    /** OHD-03: recalculate absorption rate when the allocation base volume is updated. */
    public record RecalculateRequest(
            @NotNull(message = "poolTotal is required") BigDecimal poolTotal,
            @NotNull(message = "allocationBaseVolume is required")
            @DecimalMin(value = "0.0", inclusive = false, message = "allocationBaseVolume must be > 0")
            BigDecimal allocationBaseVolume,
            String justification
    ) {
    }

    public record Response(
            String recordId, String code, String description, BigDecimal rate, String uom, String plantId,
            LocalDate effectiveFrom, LocalDate effectiveTo, Integer versionNumber, MasterDataStatus status,
            String poolCategory, String allocationBase, String period,
            String createdBy, Instant createdAt, String updatedBy, Instant updatedAt
    ) {
    }
}
