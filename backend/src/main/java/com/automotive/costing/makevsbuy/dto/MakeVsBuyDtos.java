package com.automotive.costing.makevsbuy.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class MakeVsBuyDtos {

    private MakeVsBuyDtos() {
    }

    public record CreateRequest(
            String label,
            String selectedBuildUpId,
            List<String> buyQuoteIds,
            String mixedCostId,
            String notes
    ) {
    }

    public record SourcesRequest(
            String selectedBuildUpId,
            List<String> buyQuoteIds,
            String mixedCostId
    ) {
    }

    public record DecideRequest(
            String decisionCode,
            String decisionReason
    ) {
    }

    public record LegResponse(
            int legSeq,
            String strategyType,
            String sourceRefId,
            String supplierId,
            BigDecimal unitCostSnapshot,
            String currencySnapshot,
            BigDecimal tcoUnitSnapshot,
            String tcoStatus,
            String eligibilityStatus,
            String eligibilityNotes,
            String riskSummary,
            String dataStatus
    ) {
    }

    public record DecisionExportPackage(
            String exportType,
            String simulationId,
            String programId,
            String bomNodeId,
            String status,
            String recommendationCode,
            String decisionCode,
            String decisionReason,
            String decidedAt,
            boolean bomFlagApplied,
            String reportingCurrency,
            List<LegResponse> legs,
            String generatedAt,
            List<String> notes
    ) {
    }

    public record SimulationResponse(
            String simulationId,
            String programId,
            String bomNodeId,
            String label,
            String status,
            String selectedBuildUpId,
            String mixedCostId,
            String recommendationCode,
            String recommendationSummary,
            String decisionCode,
            String decisionReason,
            String decidedBy,
            Instant decidedAt,
            boolean bomFlagApplied,
            Instant bomFlagAppliedAt,
            Instant frozenAt,
            String notes,
            List<LegResponse> legs,
            List<String> dataQualityNotes
    ) {
    }
}
