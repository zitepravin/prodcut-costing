package com.automotive.costing.procurement.dto;

import com.automotive.costing.procurement.entity.AlternativeType;
import com.automotive.costing.procurement.entity.ProcurementSetStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ProcurementDtos {
    private ProcurementDtos() {}

    public record AlternativeLineResponse(
            String lineId,
            int sequenceNo,
            AlternativeType alternativeType,
            String sourceRefType,
            String sourceRefId,
            String supplierId,
            String supplierName,
            BigDecimal unitCost,
            String currencyCode,
            BigDecimal tcoUnit,
            String tcoStatus,
            String eligibility,
            Integer rankNo,
            String dataStatus,
            String notes
    ) {}

    public record SetResponse(
            String setId,
            String bomNodeId,
            String programId,
            String label,
            ProcurementSetStatus status,
            String recommendationCode,
            String recommendationSummary,
            String decisionCode,
            String decisionReason,
            Instant decidedAt,
            List<AlternativeLineResponse> lines
    ) {}

    public record EvaluateRequest(
            @NotBlank String bomNodeId,
            String programId,
            String label,
            /** Optional manual SUBCONTRACT / EXTERNAL unit costs when no quote exists. */
            BigDecimal subcontractUnitCost,
            BigDecimal externalProcessUnitCost,
            String subcontractSupplierName,
            String externalSupplierName
    ) {}

    public record DecisionRequest(
            @NotBlank String decisionCode,
            String decisionReason
    ) {}
}
