package com.automotive.costing.financial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class FinancialDtos {
    private FinancialDtos() {}

    public record ActualCostResponse(
            String actualCostId,
            String bomNodeId,
            String programId,
            String sourceSystem,
            String sourceRef,
            String periodCode,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal totalCost,
            String currencyCode,
            BigDecimal materialCost,
            BigDecimal processCost,
            BigDecimal overheadCost,
            BigDecimal otherCost,
            LocalDate postingDate,
            Instant importedAt,
            String notes
    ) {}

    public record ImportActualRequest(
            @NotBlank String bomNodeId,
            String programId,
            String sourceSystem,
            String sourceRef,
            String periodCode,
            BigDecimal quantity,
            @NotNull BigDecimal unitCost,
            BigDecimal totalCost,
            String currencyCode,
            BigDecimal materialCost,
            BigDecimal processCost,
            BigDecimal overheadCost,
            BigDecimal otherCost,
            LocalDate postingDate,
            String notes
    ) {}

    public record BucketAmount(
            String bucket, // PLANNED | STANDARD | SHOULD | ACTUAL | SAP
            BigDecimal unitCost,
            String currencyCode,
            String sourceRef,
            String dataStatus, // OK | DATA_NOT_AVAILABLE
            String notes
    ) {}

    public record TriangleCompareResponse(
            String bomNodeId,
            String reportingCurrency,
            LocalDate valuationDate,
            List<BucketAmount> buckets,
            BigDecimal standardVsShould,
            BigDecimal standardVsActual,
            BigDecimal shouldVsActual,
            String summary
    ) {}

    public record ValuationPolicyResponse(
            String policyId,
            String policyCode,
            String policyName,
            String companyCurrency,
            String groupCurrency,
            String transactionCurrency,
            String costingCurrency,
            String valuationDatePolicy,
            LocalDate fixedValuationDate,
            boolean active,
            String notes
    ) {}

    public record CreateValuationPolicyRequest(
            @NotBlank String policyCode,
            @NotBlank String policyName,
            @NotBlank String companyCurrency,
            String groupCurrency,
            String transactionCurrency,
            @NotBlank String costingCurrency,
            String valuationDatePolicy,
            LocalDate fixedValuationDate,
            String notes
    ) {}

    public record ValuationConvertRequest(
            @NotBlank String policyCode,
            @NotNull BigDecimal amount,
            @NotBlank String fromCurrency,
            LocalDate costingDate,
            LocalDate documentDate
    ) {}

    public record ValuationConvertResponse(
            String policyCode,
            BigDecimal originalAmount,
            String fromCurrency,
            BigDecimal companyAmount,
            String companyCurrency,
            BigDecimal groupAmount,
            String groupCurrency,
            BigDecimal costingAmount,
            String costingCurrency,
            LocalDate valuationDateUsed,
            String notes
    ) {}
}
