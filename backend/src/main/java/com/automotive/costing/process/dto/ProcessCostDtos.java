package com.automotive.costing.process.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ProcessCostDtos {

    public record CalculateRequest(
            @NotBlank(message = "routingId is required") String routingId,
            /** Explicit costing/valuation date for FX and rate selection. Null = request-boundary date. */
            LocalDate costingDate
    ) {
        public CalculateRequest(String routingId) {
            this(routingId, null);
        }
    }

    public record ComponentLine(
            String componentCode,
            BigDecimal amount,
            String currency,
            BigDecimal quantity,
            String uom,
            BigDecimal rate,
            String rateSource,
            String formulaCode,
            String explanation
    ) {
    }

    public record OperationCostLine(
            String operationId,
            BigDecimal runCost,
            BigDecimal setupCostPerPiece,
            BigDecimal totalOperationCost,
            BigDecimal setupCost,
            BigDecimal machineCost,
            BigDecimal directLaborCost,
            BigDecimal powerCost,
            BigDecimal consumableCost,
            BigDecimal inspectionCost,
            BigDecimal externalProcessCost,
            List<ComponentLine> components
    ) {
        /** Backward-compatible 4-arg form for older tests. */
        public OperationCostLine(String operationId, BigDecimal runCost, BigDecimal setupCostPerPiece,
                                 BigDecimal totalOperationCost) {
            this(operationId, runCost, setupCostPerPiece, totalOperationCost,
                    setupCostPerPiece, null, null, null, null, null, null, List.of());
        }
    }

    public record Response(
            String calculationId,
            String routingId,
            String bomNodeId,
            BigDecimal totalProcessCost,
            List<OperationCostLine> operationCosts,
            BigDecimal setupCost,
            BigDecimal machineCost,
            BigDecimal directLaborCost,
            BigDecimal powerCost,
            BigDecimal consumableCost,
            BigDecimal inspectionCost,
            BigDecimal externalProcessCost,
            List<ComponentLine> components,
            LocalDate costingDate,
            String calculationBasis,
            Instant calculatedAt
    ) {
    }

    public record CompareRequest(
            @NotBlank(message = "at least two routingIds are required to compare") List<String> routingIds,
            LocalDate costingDate
    ) {
        public CompareRequest(List<String> routingIds) {
            this(routingIds, null);
        }
    }

    public record CompareLine(
            String routingId,
            BigDecimal totalProcessCost,
            int operationCount
    ) {
    }

    public record CompareResponse(
            List<CompareLine> lines,
            String lowestCostRoutingId
    ) {
    }
}
