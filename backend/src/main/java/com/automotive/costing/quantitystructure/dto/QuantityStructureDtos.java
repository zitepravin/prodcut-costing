package com.automotive.costing.quantitystructure.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public final class QuantityStructureDtos {
    private QuantityStructureDtos() {}

    public record EstimateRequest(
            @NotBlank String bomNodeId,
            String costingVariantId,
            String routingId,
            /** Overrides variant lot size when set. */
            BigDecimal lotSize
    ) {}

    public record OperationLine(
            String operationId,
            Integer sequenceNumber,
            String processName,
            String machineCode,
            String laborCode,
            BigDecimal cycleTimeSeconds,
            BigDecimal setupTimeSeconds,
            BigDecimal batchSizeUsed,
            BigDecimal machineHours,
            BigDecimal laborHours,
            BigDecimal runCost,
            BigDecimal setupCostPerPiece,
            BigDecimal totalOperationCost
    ) {}

    public record MaterialLine(
            String materialCode,
            BigDecimal netWeight,
            BigDecimal scrapPctApplied,
            BigDecimal yieldPctApplied,
            BigDecimal grossWeight,
            BigDecimal unitRate,
            BigDecimal materialCost,
            String basis
    ) {}

    public record EstimateResponse(
            String bomNodeId,
            String routingId,
            String costingVariantId,
            String variantCode,
            BigDecimal lotSizeUsed,
            BigDecimal scrapPctUsed,
            BigDecimal yieldPctUsed,
            String quantityStructureSource,
            MaterialLine material,
            List<OperationLine> operations,
            BigDecimal totalMaterialCost,
            BigDecimal totalProcessCost,
            BigDecimal totalQuantityStructureCost,
            String calculationBasis
    ) {}
}
