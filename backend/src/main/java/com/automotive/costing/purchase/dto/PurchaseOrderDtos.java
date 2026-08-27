package com.automotive.costing.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseOrderDtos {
    private PurchaseOrderDtos() {}

    public record PoLineInput(
            String materialCode,
            String materialDescription,
            @NotNull BigDecimal quantity,
            String uom,
            BigDecimal unitPrice,
            String currencyCode,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            LocalDate deliveryDate,
            String plantCode,
            String storageLocation,
            String lineNotes
    ) {}

    public record CreatePurchaseOrderRequest(
            String purchaseRequestId,
            String linkedRfqId,
            @NotBlank String supplierId,
            String plantCode,
            String buyerUserId,
            String currencyCode,
            String paymentTerms,
            String incoterms,
            String deliveryTerms,
            LocalDate orderDate,
            LocalDate requiredDeliveryDate,
            String notes,
            @NotEmpty @Valid List<PoLineInput> lines
    ) {}

    public record DecidePoRequest(
            @NotBlank String decisionCode,
            @NotBlank String decisionReason
    ) {}

    public record IssuePoRequest(
            String externalPoReference
    ) {}

    public record PoLineResponse(
            String lineId,
            int lineNo,
            String materialCode,
            String materialDescription,
            BigDecimal quantity,
            String uom,
            BigDecimal unitPrice,
            String currencyCode,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            LocalDate deliveryDate,
            String plantCode,
            String storageLocation,
            String dataStatus,
            String lineNotes
    ) {}

    public record PurchaseOrderResponse(
            String purchaseOrderId,
            String poNumber,
            String customerId,
            String purchaseRequestId,
            String linkedRfqId,
            String supplierId,
            String plantCode,
            String buyerUserId,
            String currencyCode,
            String paymentTerms,
            String incoterms,
            String deliveryTerms,
            LocalDate orderDate,
            LocalDate requiredDeliveryDate,
            String status,
            String decisionReason,
            String decidedBy,
            Instant decidedAt,
            Instant issuedAt,
            String issuedBy,
            String externalPoReference,
            String notes,
            List<PoLineResponse> lines,
            List<String> messages
    ) {}

    /** Create PO draft from an APPROVED / RFQ_LINKED / RELEASED purchase request. */
    public record CreateFromPurchaseRequestRequest(
            @NotBlank String supplierId,
            String plantCode,
            String currencyCode,
            String paymentTerms,
            String incoterms,
            String notes
    ) {}


    public record ClosePoRequest(
            String reason,
            boolean partial
    ) {}
}
