package com.automotive.costing.material.dto;

import com.automotive.costing.material.entity.PricingBasis;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class MaterialCostDtos {

    /**
     * MAT-01/02/03: request to calculate (and persist) the material cost for a BOM node.
     * netWeightSource/manualEntryApprovedBy implement the SRS 4.3.5 business rule: "Net
     * weight is sourced from the CAD/PLM part attribute wherever available; manual entry
     * requires supervisor approval." scrapPctOverrideJustification implements the sibling
     * rule: "Scrap percentage defaults from the process master...but may be overridden per
     * part with justification."
     */
    public record CalculateRequest(
            @NotBlank(message = "bomNodeId is required") String bomNodeId,
            @NotBlank(message = "materialCode is required") String materialCode,
            @NotNull(message = "netWeight is required")
            @DecimalMin(value = "0.0", inclusive = false, message = "netWeight must be > 0")
            BigDecimal netWeight,
            @NotNull(message = "netWeightSource is required") NetWeightSource netWeightSource,
            String manualEntryApprovedBy, // required if netWeightSource == MANUAL
            BigDecimal scrapPctOverride, // fraction 0<=x<1; null => default from process master
            String processName, // used to look up the default scrap% when scrapPctOverride is null
            String scrapPctOverrideJustification, // required if scrapPctOverride is supplied
            LocalDate effectiveDateOverride // null => use "today" for index/rate lookups
    ) {
        public CalculateRequest {
            // Compact constructor left formula-free deliberately - cross-field validation
            // (netWeightSource==MANUAL requires manualEntryApprovedBy, an override requires
            // a justification) is enforced in MaterialCostService, not here, so the
            // BusinessRuleViolationException carries the correct SRS rule reference rather
            // than a generic Bean Validation message.
        }
    }

    public enum NetWeightSource {
        CAD_PLM,
        MANUAL
    }

    public record Response(
            String calculationId,
            String bomNodeId,
            String materialCode,
            BigDecimal netWeight,
            BigDecimal scrapPct,
            BigDecimal grossWeight,
            BigDecimal unitRate,
            PricingBasis pricingBasis,
            BigDecimal scrapRecoveryCredit,
            BigDecimal materialCost,
            String calculationBasis,
            Instant calculatedAt
    ) {
    }

    /** MAT-04: compare material cost across >= 3 alternate grades/suppliers in one view. */
    public record CompareRequest(
            @NotNull(message = "netWeight is required")
            @DecimalMin(value = "0.0", inclusive = false, message = "netWeight must be > 0")
            BigDecimal netWeight,
            BigDecimal scrapPct,
            @NotNull(message = "at least 2 material codes are required to compare")
            List<@NotBlank String> materialCodes
    ) {
    }

    public record CompareLine(
            String materialCode,
            String grade,
            BigDecimal unitRate,
            PricingBasis pricingBasis,
            BigDecimal materialCost
    ) {
    }

    public record CompareResponse(
            BigDecimal netWeight,
            BigDecimal scrapPct,
            List<CompareLine> lines,
            String lowestCostMaterialCode
    ) {
    }

    /** MAT-07: 24-month historical price trend per material grade. */
    public record PriceTrendPoint(
            LocalDate effectiveFrom,
            Integer versionNumber,
            BigDecimal rate
    ) {
    }

    public record PriceTrendResponse(
            String materialCode,
            List<PriceTrendPoint> points
    ) {
    }

    /** MAT-08: flag a part where material cost exceeds a configurable % of total part cost. */
    public record HighMaterialCostCheckRequest(
            @NotNull BigDecimal materialCost,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal totalPartCost,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal thresholdFraction
    ) {
    }

    public record HighMaterialCostCheckResponse(
            BigDecimal materialCostRatio,
            boolean flagged
    ) {
    }
}
