package com.automotive.costing.variance.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public final class VarianceDtos {
    private VarianceDtos() {}

    public record CompareRequest(
            @NotBlank String bomNodeId,
            /** SHOULD_COST | TARGET | QUOTE | PREVIOUS_SHOULD_COST */
            @NotBlank String baselineType,
            String baselineId,
            @NotBlank String comparisonType,
            String comparisonId,
            BigDecimal comparisonTotal
    ) {}

    public record DriverResponse(
            String driverCode, String driverName,
            BigDecimal baselineAmount, BigDecimal comparisonAmount,
            BigDecimal absoluteVariance, BigDecimal percentVariance, String explanation
    ) {}

    public record CompareResponse(
            String bomNodeId,
            String baselineType,
            String comparisonType,
            BigDecimal baselineTotal,
            BigDecimal comparisonTotal,
            BigDecimal absoluteVariance,
            BigDecimal percentVariance,
            List<DriverResponse> drivers,
            String summary
    ) {}
}
