package com.automotive.costing.purchase.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class OrderTrackingDtos {
    private OrderTrackingDtos() {}

    /** CPI / middleware inbound — system of record is ERP; we only store a mirror. */
    public record InboundTrackingRequest(
            @NotBlank String purchaseRequestId,
            String purchaseRequestLineId,
            String externalPoReference,
            String trackingStatus,
            LocalDate asnDate,
            BigDecimal qtyShipped,
            BigDecimal qtyOrdered,
            String uom,
            String sourceEventId,
            String notes
    ) {}

    public record HumanCorrectionRequest(
            LocalDate asnDate,
            BigDecimal qtyShipped,
            String trackingStatus,
            String uom,
            @NotBlank String correctionReason,
            String notes
    ) {}

    public record TrackingResponse(
            String trackingId,
            String purchaseRequestId,
            String purchaseRequestLineId,
            String externalPoReference,
            String trackingStatus,
            LocalDate asnDate,
            BigDecimal qtyShipped,
            BigDecimal qtyOrdered,
            String uom,
            String sourceSystem,
            String sourceEventId,
            String dataStatus,
            boolean humanCorrected,
            String correctionReason,
            String correctedBy,
            Instant correctedAt,
            String notes,
            List<String> policyNotes
    ) {}
}
