package com.automotive.costing.costingvariant.dto;

import com.automotive.costing.costingvariant.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class CostingVariantDtos {
    private CostingVariantDtos() {}

    public record VariantResponse(
            String variantId,
            String variantCode,
            String variantName,
            CostingVariantStatus status,
            ValuationStrategy valuationStrategy,
            PriceSource materialPriceSource,
            PriceSource activityPriceSource,
            QuantityStructureSource quantityStructureSource,
            String currencyCode,
            BigDecimal lotSize,
            BigDecimal scrapPct,
            BigDecimal yieldPct,
            String overheadPoolCode,
            String costComponentProfile,
            String costingSheetCode,
            LocalDate validFrom,
            LocalDate validTo,
            String description,
            Instant updatedAt
    ) {}

    public record CreateVariantRequest(
            @NotBlank String variantCode,
            @NotBlank String variantName,
            ValuationStrategy valuationStrategy,
            PriceSource materialPriceSource,
            PriceSource activityPriceSource,
            QuantityStructureSource quantityStructureSource,
            String currencyCode,
            BigDecimal lotSize,
            BigDecimal scrapPct,
            BigDecimal yieldPct,
            String overheadPoolCode,
            String costComponentProfile,
            String costingSheetCode,
            LocalDate validFrom,
            LocalDate validTo,
            String description
    ) {}

    public record UpdateVariantRequest(
            String variantName,
            ValuationStrategy valuationStrategy,
            PriceSource materialPriceSource,
            PriceSource activityPriceSource,
            QuantityStructureSource quantityStructureSource,
            String currencyCode,
            BigDecimal lotSize,
            BigDecimal scrapPct,
            BigDecimal yieldPct,
            String overheadPoolCode,
            String costComponentProfile,
            String costingSheetCode,
            LocalDate validFrom,
            LocalDate validTo,
            String description
    ) {}
}
