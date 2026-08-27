package com.automotive.costing.purchase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DeliveryPerformanceDtos {
    private DeliveryPerformanceDtos() {}

    public record ShipmentPerformance(
            String trackingId,
            String purchaseRequestId,
            String purchaseRequestLineId,
            String supplierId,
            String trackingStatus,
            LocalDate needByDate,
            LocalDate asnDate,
            BigDecimal qtyOrdered,
            BigDecimal qtyShipped,
            /** Positive = late days; negative = early; null if NOT_AVAILABLE */
            Long delayDays,
            /** true/false if computable; null if DATA NOT AVAILABLE */
            Boolean onTimeComputed,
            Boolean onTimeEffective,
            boolean humanOverrideApplied,
            String onTimeOverrideReason,
            String dataStatus,
            BigDecimal qtyVariance,
            String qtyVarianceStatus,
            List<String> notes
    ) {}

    public record PerformanceSummary(
            String purchaseRequestId,
            String programId,
            String status,
            int shipmentCount,
            int onTimeCount,
            int lateCount,
            int dataNotAvailableCount,
            List<ShipmentPerformance> shipments,
            List<String> policyNotes
    ) {}

    public record OnTimeOverrideRequest(
            @NotNull Boolean onTimeOverride,
            @NotBlank String reason
    ) {}
}
