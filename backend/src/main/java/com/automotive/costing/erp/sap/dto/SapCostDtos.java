package com.automotive.costing.erp.sap.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class SapCostDtos {

    private SapCostDtos() {
    }

    public record ComponentIn(
            String sapComponentCode,
            String sapComponentName,
            BigDecimal amount
    ) {
    }

    public record LandRequest(
            String sourceSystem,
            String sapMaterial,
            String sapPlant,
            String costingVariant,
            String costingVersion,
            LocalDate estimateDate,
            String currencyCode,
            BigDecimal totalCost,
            String rawPayload,
            List<ComponentIn> components
    ) {
    }

    public record MappingUpsert(
            String sapComponentCode,
            String ourComponentCode,
            String notes
    ) {
    }

    public record MappingResponse(
            String mappingId,
            String sapComponentCode,
            String ourComponentCode,
            String notes,
            boolean activeFlag
    ) {
    }

    public record ComponentOut(
            int componentSeq,
            String sapComponentCode,
            String sapComponentName,
            BigDecimal amount,
            String mappedComponentCode
    ) {
    }

    public record StagingResponse(
            String stagingId,
            String sourceSystem,
            String sapMaterial,
            String sapPlant,
            String costingVariant,
            String costingVersion,
            LocalDate estimateDate,
            String currencyCode,
            BigDecimal totalCost,
            String status,
            String shouldCostBuildUpId,
            String bomNodeId,
            Instant receivedAt,
            Instant comparedAt,
            List<ComponentOut> components,
            List<String> messages
    ) {
    }

    public record CompareRequest(
            String shouldCostBuildUpId
    ) {
    }

    public record ComponentVariance(
            String code,
            BigDecimal sapAmount,
            BigDecimal shouldAmount,
            BigDecimal variance,
            String status
    ) {
    }

    public record CompareResponse(
            String stagingId,
            String totalStatus,
            BigDecimal sapTotal,
            BigDecimal shouldTotal,
            BigDecimal absoluteVariance,
            BigDecimal percentageVariance,
            String shouldCostBuildUpId,
            List<ComponentVariance> components,
            List<String> dataQualityNotes
    ) {
    }
}
