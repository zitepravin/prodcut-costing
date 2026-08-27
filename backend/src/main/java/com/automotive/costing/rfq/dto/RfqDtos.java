package com.automotive.costing.rfq.dto;

import com.automotive.costing.rfq.entity.QuoteStatus;
import com.automotive.costing.rfq.entity.RfqStatus;
import com.automotive.costing.rfq.entity.SupplierStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class RfqDtos {

    private RfqDtos() {}

    public record CreateSupplierRequest(
            @NotBlank @Size(max = 50) String supplierCode,
            @NotBlank @Size(max = 200) String supplierName,
            @Size(max = 3) String countryCode,
            @Size(max = 3) String currencyCode,
            @Size(max = 500) String notes
    ) {}

    public record SupplierResponse(
            String supplierId,
            String supplierCode,
            String supplierName,
            String countryCode,
            String currencyCode,
            SupplierStatus status,
            String notes
    ) {}

    public record CreateRfqRequest(
            @NotBlank String programId,
            @NotBlank @Size(max = 50) String rfqNumber,
            @NotBlank @Size(max = 200) String title,
            LocalDate dueDate,
            @Size(max = 3) String currencyCode,
            @Size(max = 1000) String notes
    ) {}

    public record RfqResponse(
            String rfqId,
            String programId,
            String rfqNumber,
            String title,
            RfqStatus status,
            LocalDate dueDate,
            String currencyCode,
            String notes,
            Instant createdAt
    ) {}

    public record AddRfqLineRequest(
            @NotBlank String bomNodeId,
            Integer lineNumber,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
            @Size(max = 20) String uom,
            @DecimalMin(value = "0", inclusive = true) BigDecimal targetUnitPrice,
            @Size(max = 500) String notes
    ) {}

    public record RfqLineResponse(
            String rfqLineId,
            String rfqId,
            String bomNodeId,
            Integer lineNumber,
            BigDecimal quantity,
            String uom,
            BigDecimal targetUnitPrice,
            String notes
    ) {}

    public record RfqDetailResponse(
            RfqResponse rfq,
            List<RfqLineResponse> lines
    ) {}

    public record SubmitQuoteRequest(
            @NotBlank String rfqLineId,
            @NotBlank String supplierId,
            @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal unitPrice,
            @Size(max = 3) String currencyCode,
            BigDecimal toolingLumpSum,
            Integer leadTimeDays,
            LocalDate validFrom,
            LocalDate validTo,
            @Size(max = 100) String negotiationReference,
            @Size(max = 1000) String notes
    ) {}

    public record QuoteResponse(
            String quoteId,
            String rfqId,
            String rfqLineId,
            String supplierId,
            String bomNodeId,
            BigDecimal unitPrice,
            String currencyCode,
            BigDecimal toolingLumpSum,
            Integer leadTimeDays,
            LocalDate validFrom,
            LocalDate validTo,
            QuoteStatus status,
            String negotiationReference,
            String notes
    ) {}

    public record UpdateQuoteStatusRequest(
            @NotNull QuoteStatus status,
            @Size(max = 100) String negotiationReference
    ) {}

    /** Buyer analysis: each quote vs latest should-cost on the BOM node (no invented FX). */
    public record QuoteShouldCostComparisonRow(
            String quoteId,
            String rfqLineId,
            String supplierId,
            String bomNodeId,
            BigDecimal quoteUnitPrice,
            String quoteCurrency,
            QuoteStatus quoteStatus,
            BigDecimal shouldCostTotal,
            String shouldCostBuildUpId,
            String shouldCostScenarioLabel,
            String shouldCostApprovalStatus,
            String comparisonStatus,
            BigDecimal varianceAmount,
            String varianceNote
    ) {}

    public record QuoteShouldCostComparisonResponse(
            String rfqId,
            List<QuoteShouldCostComparisonRow> rows,
            List<String> messages
    ) {}
}
