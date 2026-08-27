package com.automotive.costing.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CommodityIndexDtos {

    public record PublishRequest(
            @NotBlank(message = "indexCode is required") String indexCode,
            @NotNull(message = "indexValue is required") BigDecimal indexValue,
            @NotNull(message = "effectiveDate is required") LocalDate effectiveDate,
            String source
    ) {
    }

    public record Response(
            String indexValueId,
            String indexCode,
            BigDecimal indexValue,
            LocalDate effectiveDate,
            String source
    ) {
    }
}
