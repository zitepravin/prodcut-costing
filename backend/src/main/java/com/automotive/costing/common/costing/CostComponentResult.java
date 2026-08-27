package com.automotive.costing.common.costing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Traceable cost component result (V277 Costing Engine V2).
 * Extends the conceptual model behind CostComponentDefinition / ShouldCost breakdown
 * without replacing persisted definition tables.
 */
public record CostComponentResult(
        String componentCode,
        BigDecimal quantity,
        String uom,
        BigDecimal rate,
        String rateCurrency,
        BigDecimal amount,
        String currency,
        String rateSource,
        LocalDate rateEffectiveDate,
        String formulaCode,
        String explanation,
        String sourceReference,
        String costingRunId,
        String costingVariantId,
        String bomNodeId,
        String operationId,
        // FX metadata (null when sourceCurrency == targetCurrency / identity)
        String sourceCurrency,
        String targetCurrency,
        BigDecimal appliedFxRate,
        LocalDate fxAsOfDate,
        LocalDate fxRateEffectiveDate,
        String fxConversionMethod,
        String fxRateSource
) {
    public static Builder builder(String componentCode) {
        return new Builder(componentCode);
    }

    public static final class Builder {
        private final String componentCode;
        private BigDecimal quantity;
        private String uom;
        private BigDecimal rate;
        private String rateCurrency;
        private BigDecimal amount;
        private String currency;
        private String rateSource;
        private LocalDate rateEffectiveDate;
        private String formulaCode;
        private String explanation;
        private String sourceReference;
        private String costingRunId;
        private String costingVariantId;
        private String bomNodeId;
        private String operationId;
        private String sourceCurrency;
        private String targetCurrency;
        private BigDecimal appliedFxRate;
        private LocalDate fxAsOfDate;
        private LocalDate fxRateEffectiveDate;
        private String fxConversionMethod;
        private String fxRateSource;

        private Builder(String componentCode) {
            this.componentCode = componentCode;
        }

        public Builder quantity(BigDecimal v) { this.quantity = v; return this; }
        public Builder uom(String v) { this.uom = v; return this; }
        public Builder rate(BigDecimal v) { this.rate = v; return this; }
        public Builder rateCurrency(String v) { this.rateCurrency = v; return this; }
        public Builder amount(BigDecimal v) { this.amount = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder rateSource(String v) { this.rateSource = v; return this; }
        public Builder rateEffectiveDate(LocalDate v) { this.rateEffectiveDate = v; return this; }
        public Builder formulaCode(String v) { this.formulaCode = v; return this; }
        public Builder explanation(String v) { this.explanation = v; return this; }
        public Builder sourceReference(String v) { this.sourceReference = v; return this; }
        public Builder costingRunId(String v) { this.costingRunId = v; return this; }
        public Builder costingVariantId(String v) { this.costingVariantId = v; return this; }
        public Builder bomNodeId(String v) { this.bomNodeId = v; return this; }
        public Builder operationId(String v) { this.operationId = v; return this; }
        public Builder sourceCurrency(String v) { this.sourceCurrency = v; return this; }
        public Builder targetCurrency(String v) { this.targetCurrency = v; return this; }
        public Builder appliedFxRate(BigDecimal v) { this.appliedFxRate = v; return this; }
        public Builder fxAsOfDate(LocalDate v) { this.fxAsOfDate = v; return this; }
        public Builder fxRateEffectiveDate(LocalDate v) { this.fxRateEffectiveDate = v; return this; }
        public Builder fxConversionMethod(String v) { this.fxConversionMethod = v; return this; }
        public Builder fxRateSource(String v) { this.fxRateSource = v; return this; }

        public CostComponentResult build() {
            return new CostComponentResult(
                    componentCode, quantity, uom, rate, rateCurrency, amount, currency,
                    rateSource, rateEffectiveDate, formulaCode, explanation, sourceReference,
                    costingRunId, costingVariantId, bomNodeId, operationId,
                    sourceCurrency, targetCurrency, appliedFxRate, fxAsOfDate,
                    fxRateEffectiveDate, fxConversionMethod, fxRateSource);
        }
    }
}
