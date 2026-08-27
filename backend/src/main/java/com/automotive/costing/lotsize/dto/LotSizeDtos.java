package com.automotive.costing.lotsize.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public final class LotSizeDtos {
    private LotSizeDtos() {}

    public record CurveRequest(
            @NotBlank String bomNodeId,
            String costingVariantId,
            String routingId,
            List<BigDecimal> lotSizes
    ) {}

    public record CurvePoint(
            BigDecimal lotSize,
            BigDecimal unitCost,
            BigDecimal materialCost,
            BigDecimal processCost,
            String status,
            String message
    ) {}

    public record CurveResponse(
            String bomNodeId,
            List<CurvePoint> points,
            BigDecimal lowestUnitCostLotSize,
            String calculationBasis
    ) {}
}
