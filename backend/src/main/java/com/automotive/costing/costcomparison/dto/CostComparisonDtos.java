package com.automotive.costing.costcomparison.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated cost comparison workspace for a single BOM node.
 * Composes existing M08 scenarios, M10 accepted quotes, M06 tooling calcs, and target costs
 * (no new costing engines — read-only aggregation of established module outputs).
 */
public final class CostComparisonDtos {

    private CostComparisonDtos() {}

    public record ShouldCostLine(
            String buildUpId,
            String scenarioLabel,
            BigDecimal totalCost,
            String status
    ) {}

    public record QuoteLine(
            String quoteId,
            String supplierId,
            BigDecimal unitPrice,
            String currencyCode,
            String status,
            Integer leadTimeDays
    ) {}

    public record ToolingLine(
            String calculationId,
            String toolingRecordId,
            BigDecimal amortizedCostPerPiece,
            boolean includedInPiecePrice
    ) {}

    public record TargetLine(
            String targetCostId,
            BigDecimal targetAmount,
            String currencyCode,
            String status,
            String approvalStatus
    ) {}

    public record WorkspaceResponse(
            String bomNodeId,
            List<ShouldCostLine> shouldCostScenarios,
            String lowestShouldCostBuildUpId,
            List<QuoteLine> supplierQuotes,
            String lowestAcceptedQuoteId,
            List<ToolingLine> toolingCosts,
            List<TargetLine> targets,
            BigDecimal activeTargetAmount,
            BigDecimal lowestShouldCost,
            BigDecimal lowestAcceptedQuoteUnitPrice
    ) {}
}
