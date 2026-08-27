package com.automotive.costing.mixedcost.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class MixedCostDtos {

    private MixedCostDtos() {
    }

    public record LegRequest(
            String alternativeType,
            String sourceRefId,
            BigDecimal mixRatio
    ) {
    }

    public record CreateRequest(
            String versionLabel,
            String notes,
            List<LegRequest> legs
    ) {
    }

    public record ReplaceLegsRequest(
            List<LegRequest> legs
    ) {
    }

    public record LegResponse(
            int legSeq,
            String alternativeType,
            String sourceRefId,
            String supplierId,
            BigDecimal mixRatio,
            BigDecimal unitCostSnapshot,
            String currencySnapshot,
            String eligibilityStatus,
            String eligibilityNotes,
            String dataStatus
    ) {
    }

    public record MixedCostResponse(
            String mixedCostId,
            String programId,
            String bomNodeId,
            String versionLabel,
            String status,
            String reportingCurrency,
            BigDecimal blendedUnitTotal,
            String blendedStatus,
            BigDecimal ratioSumCheck,
            String notes,
            Instant calculatedAt,
            Instant frozenAt,
            List<LegResponse> legs,
            List<String> dataQualityNotes
    ) {
    }
}
