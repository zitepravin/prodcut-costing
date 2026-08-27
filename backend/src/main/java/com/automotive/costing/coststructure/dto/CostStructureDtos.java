package com.automotive.costing.coststructure.dto;

import java.math.BigDecimal;
import java.util.List;

public final class CostStructureDtos {
    private CostStructureDtos() {}

    public record ComponentNode(
            String componentCode,
            String displayName,
            String category,
            String costBehavior,
            int hierarchyLevel,
            int sequenceNo,
            String mapsToColumn,
            List<ComponentNode> children
    ) {}

    public record ComponentStructureResponse(List<ComponentNode> roots) {}

    public record CostedBomNode(
            String bomNodeId,
            String partNumber,
            String revision,
            String parentNodeId,
            BigDecimal quantityPer,
            String uom,
            String makeBuyFlag,
            String materialCode,
            BigDecimal scrapPct,
            BigDecimal unitCost,
            BigDecimal extendedCost,
            BigDecimal materialCost,
            BigDecimal processCost,
            BigDecimal toolingCost,
            BigDecimal packagingFreightCost,
            BigDecimal overheadCost,
            String costSource,
            boolean overrideFlag,
            List<CostedBomNode> children
    ) {}

    public record MultilevelCostedBomResponse(
            String rootBomNodeId,
            String programId,
            BigDecimal totalRolledCost,
            CostedBomNode tree
    ) {}

    public record ItemizationLine(
            String itemId,
            int lineNo,
            String componentCode,
            String description,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal rate,
            BigDecimal amount,
            String currencyCode,
            String costBehavior,
            String priceSource,
            String calculationRule
    ) {}

    public record ItemizationResponse(
            String estimateRefType,
            String estimateRefId,
            String bomNodeId,
            List<ItemizationLine> lines,
            BigDecimal totalAmount,
            BigDecimal totalFixed,
            BigDecimal totalVariable
    ) {}
}
