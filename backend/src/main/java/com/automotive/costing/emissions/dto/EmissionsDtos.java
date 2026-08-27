package com.automotive.costing.emissions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class EmissionsDtos {
    private EmissionsDtos() {}

    public record CreateFactorRequest(
            @NotBlank String factorCode,
            @NotBlank String factorName,
            @NotNull BigDecimal co2eValue,
            String unit,
            String category, // MATERIAL | PROCESS
            String sourceType,
            String sourceReference,
            String geography,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {}

    public record FactorResponse(
            String factorId,
            String factorCode,
            String factorName,
            String category,
            BigDecimal co2eValue,
            String unit,
            String sourceType,
            String sourceReference,
            String geography,
            int versionNo,
            String status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {}

    public record LinkMaterialFactorRequest(@NotBlank String factorId) {}

    public record EmissionsSnapshotResponse(
            String snapshotId,
            String buildUpId,
            String bomNodeId,
            BigDecimal materialCo2eKg,
            BigDecimal processCo2eKg,
            BigDecimal totalCo2eKg,
            String emissionsStatus,
            String factorId,
            String factorCode,
            Integer factorVersionNo,
            BigDecimal quantityKg,
            String calculationNotes,
            Instant calculatedAt,
            List<String> policyNotes
    ) {}
}
