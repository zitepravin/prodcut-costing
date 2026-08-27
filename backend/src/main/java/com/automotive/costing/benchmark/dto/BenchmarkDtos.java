package com.automotive.costing.benchmark.dto;

import com.automotive.costing.benchmark.entity.BenchmarkCategory;
import com.automotive.costing.benchmark.entity.BenchmarkPackStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class BenchmarkDtos {
    private BenchmarkDtos() {}

    public record PackResponse(
            String packId,
            String packCode,
            String packName,
            String regionCode,
            String sourceLabel,
            String versionLabel,
            BenchmarkPackStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes,
            long rateCount,
            Instant updatedAt
    ) {}

    public record RateResponse(
            String rateId,
            String packId,
            BenchmarkCategory category,
            String processFamily,
            String rateCode,
            String description,
            BigDecimal rateValue,
            String currencyCode,
            String unitOfMeasure,
            String confidence
    ) {}

    public record CreatePackRequest(
            @NotBlank String packCode,
            @NotBlank String packName,
            @NotBlank String regionCode,
            String sourceLabel,
            String versionLabel,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes
    ) {}

    public record CreateRateRequest(
            @NotNull BenchmarkCategory category,
            String processFamily,
            @NotBlank String rateCode,
            String description,
            @NotNull BigDecimal rateValue,
            @NotBlank String currencyCode,
            @NotBlank String unitOfMeasure,
            String confidence
    ) {}

    public record PackDetailResponse(PackResponse pack, List<RateResponse> rates) {}
}
