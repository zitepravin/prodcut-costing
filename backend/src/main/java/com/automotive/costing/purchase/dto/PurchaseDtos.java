package com.automotive.costing.purchase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseDtos {
    private PurchaseDtos() {}

    /**
     * Purchase-module-native create (no BOM / no Product Costing required).
     * For PURCHASE-only tenants.
     */
    public record CreateStandalonePurchaseRequest(
            String plantCode,
            String departmentCode,
            String buyerUserId,
            LocalDate requiredDate,
            String priorityCode,
            String purposeText,
            String reportingCurrency,
            String notes,
            List<LineInput> lines
    ) {}

    /** Optional Product Costing path — MvB/BOM seed when PRODUCT_COSTING is also entitled. */
    public record CreateFromMvbRequest(
            String makeVsBuySimulationId,
            String linkedRfqId,
            String reportingCurrency,
            String notes,
            String manualOverrideReason,
            String plantCode,
            String departmentCode,
            String buyerUserId,
            LocalDate requiredDate,
            String priorityCode,
            String purposeText,
            List<LineInput> lines
    ) {}

    public record LineInput(
            String supplierId,
            String supplierQuoteId,
            String materialCode,
            String materialDescription,
            @NotNull BigDecimal quantity,
            String uom,
            BigDecimal targetPrice,
            String targetCurrency,
            String costCenterCode,
            LocalDate needByDate,
            String lineNotes
    ) {}

    public record DecideRequest(
            @NotBlank String decisionCode,
            @NotBlank String decisionReason
    ) {}

    public record ReleaseRequest(
            String externalPoReference
    ) {}

    public record LineResponse(
            String lineId,
            int lineNo,
            String materialCode,
            String materialDescription,
            String supplierId,
            String supplierQuoteId,
            BigDecimal quantity,
            String uom,
            BigDecimal targetPrice,
            String targetCurrency,
            String costCenterCode,
            BigDecimal unitPrice,
            String currency,
            BigDecimal unitPriceReporting,
            BigDecimal fxRate,
            String dataStatus,
            BigDecimal tcoUnit,
            String tcoStatus,
            LocalDate needByDate,
            String lineNotes
    ) {}

    public record PurchaseRequestResponse(
            String purchaseRequestId,
            String prNumber,
            String programId,
            String bomNodeId,
            String makeVsBuySimulationId,
            String linkedRfqId,
            String status,
            String plantCode,
            String requesterUserId,
            String departmentCode,
            String buyerUserId,
            LocalDate requiredDate,
            String priorityCode,
            String purposeText,
            String reportingCurrency,
            String manualOverrideReason,
            String decisionCode,
            String decisionReason,
            String decidedBy,
            Instant decidedAt,
            Instant releasedAt,
            String externalPoReference,
            String poLinkStatus,
            String externalPoSystem,
            Instant poLinkedAt,
            String poLinkedSource,
            String notes,
            List<LineResponse> lines,
            List<String> messages
    ) {}

    public record PurchaseExportPackage(
            String purchaseRequestId,
            String prNumber,
            String status,
            String programId,
            String bomNodeId,
            String plantCode,
            String makeVsBuySimulationId,
            String linkedRfqId,
            String reportingCurrency,
            String decisionCode,
            String decisionReason,
            String decidedBy,
            Instant decidedAt,
            Instant releasedAt,
            String externalPoReference,
            List<LineResponse> lines,
            List<String> policyNotes
    ) {}
}
