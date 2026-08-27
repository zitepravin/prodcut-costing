package com.automotive.costing.currency.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Pure FX conversion engine (M12). No Spring / HTTP dependencies.
 * Supports:
 *  - IDENTITY (same currency)
 *  - DIRECT rate (from -> to)
 *  - INVERSE (to -> from, inverted)
 *  - VIA_USD triangulation when both legs exist against USD
 *
 * All arithmetic uses BigDecimal with explicit scale; never floating point.
 */
public final class CurrencyConversionEngine {

    public static final String PATH_IDENTITY = "IDENTITY";
    public static final String PATH_DIRECT = "DIRECT";
    public static final String PATH_INVERSE = "INVERSE";
    public static final String PATH_VIA_USD = "VIA_USD";

    private static final String USD = "USD";
    private static final int RATE_SCALE = 8;
    private static final int MONEY_SCALE = 6;

    private CurrencyConversionEngine() {
    }

    public record ConversionResult(
            BigDecimal convertedAmount,
            BigDecimal rateUsed,
            String conversionPath
    ) {
    }

    /**
     * @param rateLookup (from, to) -> rate if present; engine never persists
     */
    public static ConversionResult convert(
            BigDecimal amount,
            String fromCurrency,
            String toCurrency,
            BiFunction<String, String, Optional<BigDecimal>> rateLookup) {

        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(fromCurrency, "fromCurrency");
        Objects.requireNonNull(toCurrency, "toCurrency");
        Objects.requireNonNull(rateLookup, "rateLookup");

        String from = fromCurrency.trim().toUpperCase();
        String to = toCurrency.trim().toUpperCase();

        if (from.length() != 3 || to.length() != 3) {
            throw new BusinessRuleViolationException(
                    "Currency codes must be ISO 4217 3-letter codes", "CUR-01");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "Amount to convert must be >= 0", "CUR-02");
        }

        if (from.equals(to)) {
            return new ConversionResult(
                    amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ONE.setScale(RATE_SCALE, RoundingMode.HALF_UP),
                    PATH_IDENTITY);
        }

        Optional<BigDecimal> direct = rateLookup.apply(from, to);
        if (direct.isPresent()) {
            BigDecimal rate = direct.get();
            return apply(amount, rate, PATH_DIRECT);
        }

        Optional<BigDecimal> inverse = rateLookup.apply(to, from);
        if (inverse.isPresent()) {
            BigDecimal inv = inverse.get();
            if (inv.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleViolationException(
                        "Inverse rate must be > 0", "CUR-03");
            }
            // Divide by the published inverse rate (not multiply by a rounded reciprocal)
            // to avoid intermediate rounding error (e.g. 8350 INR / 83.50 = 100 USD exactly).
            BigDecimal converted = amount.divide(inv, MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal rateUsed = BigDecimal.ONE.divide(inv, RATE_SCALE, RoundingMode.HALF_UP);
            return new ConversionResult(converted, rateUsed, PATH_INVERSE);
        }

        // Triangulate via USD when neither direct nor inverse exists
        if (!from.equals(USD) && !to.equals(USD)) {
            Optional<BigDecimal> fromToUsd = rateLookup.apply(from, USD);
            if (fromToUsd.isEmpty()) {
                fromToUsd = rateLookup.apply(USD, from)
                        .map(r -> BigDecimal.ONE.divide(r, RATE_SCALE, RoundingMode.HALF_UP));
            }
            Optional<BigDecimal> usdToTarget = rateLookup.apply(USD, to);
            if (usdToTarget.isEmpty()) {
                usdToTarget = rateLookup.apply(to, USD)
                        .map(r -> BigDecimal.ONE.divide(r, RATE_SCALE, RoundingMode.HALF_UP));
            }
            if (fromToUsd.isPresent() && usdToTarget.isPresent()) {
                BigDecimal composite = fromToUsd.get()
                        .multiply(usdToTarget.get())
                        .setScale(RATE_SCALE, RoundingMode.HALF_UP);
                return apply(amount, composite, PATH_VIA_USD);
            }
        }

        throw new BusinessRuleViolationException(
                "No exchange rate available for " + from + " -> " + to
                        + " (direct, inverse, or via USD)",
                "CUR-04");
    }

    private static ConversionResult apply(BigDecimal amount, BigDecimal rate, String path) {
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Exchange rate must be > 0", "CUR-03");
        }
        BigDecimal converted = amount.multiply(rate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new ConversionResult(converted, rate.setScale(RATE_SCALE, RoundingMode.HALF_UP), path);
    }
}
