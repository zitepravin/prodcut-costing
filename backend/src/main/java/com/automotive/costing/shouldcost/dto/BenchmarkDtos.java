package com.automotive.costing.shouldcost.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BenchmarkDtos {

    public record SgaMarginCreateRequest(
            @NotBlank(message = "commodityFamily is required") String commodityFamily,
            @NotBlank(message = "region is required") String region,
            @NotNull(message = "sgaPct is required") BigDecimal sgaPct,
            @NotNull(message = "marginPct is required") BigDecimal marginPct
    ) {
    }

    public record SgaMarginResponse(
            String benchmarkId,
            String commodityFamily,
            String region,
            BigDecimal sgaPct,
            BigDecimal marginPct,
            boolean active
    ) {
    }

    public record ParametricCreateRequest(
            @NotBlank(message = "materialFamily is required") String materialFamily,
            @NotBlank(message = "processFamily is required") String processFamily,
            @NotBlank(message = "region is required") String region,
            @NotNull(message = "costPerKg is required") BigDecimal costPerKg
    ) {
    }

    public record ParametricResponse(
            String benchmarkId,
            String materialFamily,
            String processFamily,
            String region,
            BigDecimal costPerKg,
            boolean active
    ) {
    }
}
