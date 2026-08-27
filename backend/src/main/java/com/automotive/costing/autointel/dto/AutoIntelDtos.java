package com.automotive.costing.autointel.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AutoIntelDtos {
    private AutoIntelDtos() {}

    public record EcnImpactResponse(
            String impactId, String ecnNumber, String ecnTitle, String programId, String bomNodeId,
            String baselineBuildUpId, String proposedBuildUpId,
            BigDecimal baselineUnitCost, BigDecimal proposedUnitCost, BigDecimal unitDelta,
            BigDecimal annualVolume, BigDecimal annualProgramDelta, String currencyCode,
            String status, String impactSummary, String notes, Instant createdAt
    ) {}

    public record AnalyzeEcnRequest(
            @NotBlank String ecnNumber,
            String ecnTitle,
            @NotBlank String bomNodeId,
            String programId,
            String baselineBuildUpId,
            String proposedBuildUpId,
            /** Manual proposed unit if no second build-up. */
            BigDecimal proposedUnitCost,
            BigDecimal annualVolume,
            String notes
    ) {}

    public record WhatIfResponse(
            String simulationId, String bomNodeId, String programId, String label,
            BigDecimal baselineUnitCost, String baselineCurrency,
            BigDecimal materialPctChange, BigDecimal volumePctChange, BigDecimal fxPctChange,
            BigDecimal processPctChange, BigDecimal toolingPctChange,
            BigDecimal simulatedUnitCost, BigDecimal unitDelta,
            BigDecimal annualVolume, BigDecimal annualDelta,
            String calculationBasis, String status, Instant createdAt
    ) {}

    public record RunWhatIfRequest(
            @NotBlank String bomNodeId,
            String programId,
            String label,
            String baselineBuildUpId,
            BigDecimal materialPctChange,
            BigDecimal processPctChange,
            BigDecimal toolingPctChange,
            BigDecimal fxPctChange,
            BigDecimal volumePctChange,
            BigDecimal annualVolume
    ) {}

    public record BenchmarkCompareResponse(
            String bomNodeId,
            BigDecimal shouldCost,
            BigDecimal quoteCost,
            BigDecimal benchmarkRate,
            String benchmarkCategory,
            BigDecimal vsShould,
            BigDecimal vsQuote,
            String dataStatus,
            String notes
    ) {}
}
