package com.automotive.costing.currency.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class CurrencyDtos {

    private CurrencyDtos() {
    }

    public record CurrencyResponse(
            String currencyCode,
            String description,
            Integer decimalPlaces
    ) {
    }

    public record PublishRateRequest(
            @NotBlank @Size(min = 3, max = 3) String fromCurrency,
            @NotBlank @Size(min = 3, max = 3) String toCurrency,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal rate,
            @NotNull LocalDate effectiveDate,
            String source
    ) {
    }

    public record ExchangeRateResponse(
            String rateId,
            String fromCurrency,
            String toCurrency,
            BigDecimal rate,
            LocalDate effectiveDate,
            String source,
            Instant createdAt
    ) {
    }

    public record ConvertRequest(
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String fromCurrency,
            @NotBlank @Size(min = 3, max = 3) String toCurrency,
            LocalDate asOfDate // null => today
    ) {
    }

    public record ConvertResponse(
            BigDecimal originalAmount,
            String fromCurrency,
            BigDecimal convertedAmount,
            String toCurrency,
            BigDecimal rateUsed,
            LocalDate rateEffectiveDate,
            String conversionPath, // DIRECT | INVERSE | VIA_USD | IDENTITY
            String rateSource       // ExchangeRate.source when used; null for IDENTITY
    ) {
    }

    public record RateHistoryResponse(
            String fromCurrency,
            String toCurrency,
            List<ExchangeRateResponse> rates
    ) {
    }
}
