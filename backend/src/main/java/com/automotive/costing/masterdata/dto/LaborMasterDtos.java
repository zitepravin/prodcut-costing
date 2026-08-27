package com.automotive.costing.masterdata.dto;

import com.automotive.costing.masterdata.entity.MasterDataStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class LaborMasterDtos {

    public record CreateRequest(
            @NotBlank(message = "code is required") String code,
            String description,
            @NotNull(message = "rate is required") @DecimalMin(value = "0.0", message = "rate must be >= 0")
            BigDecimal rate,
            @NotBlank(message = "uom is required") String uom,
            String plantId,
            @NotNull(message = "effectiveFrom is required") LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String operatorGrade,
            String shiftType,
            String countryRegion,
            BigDecimal statutoryBurdenPct
    ) {
    }

    public record UpdateRequest(
            String description, BigDecimal rate, String uom, LocalDate effectiveFrom, LocalDate effectiveTo,
            String operatorGrade, String shiftType, String countryRegion, BigDecimal statutoryBurdenPct
    ) {
    }

    public record Response(
            String recordId, String code, String description, BigDecimal rate, String uom, String plantId,
            LocalDate effectiveFrom, LocalDate effectiveTo, Integer versionNumber, MasterDataStatus status,
            String operatorGrade, String shiftType, String countryRegion, BigDecimal statutoryBurdenPct,
            String createdBy, Instant createdAt, String updatedBy, Instant updatedAt
    ) {
    }
}
