package com.automotive.costing.currency.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.currency.engine.CurrencyConversionEngine.ConversionResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyConversionEngineTest {

    private static BiFunction<String, String, Optional<BigDecimal>> rates(Map<String, BigDecimal> map) {
        return (from, to) -> Optional.ofNullable(map.get(from + "->" + to));
    }

    @Test void identity_sameCurrency() {
        ConversionResult r = CurrencyConversionEngine.convert(new BigDecimal("100.00"), "USD", "USD", rates(Map.of()));
        assertEquals(0, new BigDecimal("100.000000").compareTo(r.convertedAmount()));
        assertEquals(CurrencyConversionEngine.PATH_IDENTITY, r.conversionPath());
    }

    @Test void direct_usd_to_inr_8450() {
        ConversionResult r = CurrencyConversionEngine.convert(new BigDecimal("100"), "USD", "INR",
                rates(Map.of("USD->INR", new BigDecimal("84.50"))));
        assertEquals(0, new BigDecimal("8450.000000").compareTo(r.convertedAmount()));
        assertEquals(CurrencyConversionEngine.PATH_DIRECT, r.conversionPath());
    }

    @Test void inverse_inr_to_usd() {
        ConversionResult r = CurrencyConversionEngine.convert(new BigDecimal("8450"), "INR", "USD",
                rates(Map.of("USD->INR", new BigDecimal("84.50"))));
        assertEquals(0, new BigDecimal("100.000000").compareTo(r.convertedAmount()));
        assertEquals(CurrencyConversionEngine.PATH_INVERSE, r.conversionPath());
    }

    @Test void viaUsd_eur_to_inr() {
        ConversionResult r = CurrencyConversionEngine.convert(new BigDecimal("100"), "EUR", "INR",
                rates(Map.of("EUR->USD", new BigDecimal("1.087"), "USD->INR", new BigDecimal("84.50"))));
        assertEquals(CurrencyConversionEngine.PATH_VIA_USD, r.conversionPath());
        assertEquals(0, new BigDecimal("9185.150000").compareTo(r.convertedAmount()));
    }

    @Test void missing_rate_throws() {
        assertThrows(BusinessRuleViolationException.class, () ->
                CurrencyConversionEngine.convert(new BigDecimal("10"), "USD", "XYZ", rates(Map.of())));
    }

    @Test void zero_rate_throws() {
        assertThrows(BusinessRuleViolationException.class, () ->
                CurrencyConversionEngine.convert(new BigDecimal("10"), "USD", "INR",
                        rates(Map.of("USD->INR", BigDecimal.ZERO))));
    }

    @Test void negative_rate_throws() {
        assertThrows(BusinessRuleViolationException.class, () ->
                CurrencyConversionEngine.convert(new BigDecimal("10"), "USD", "INR",
                        rates(Map.of("USD->INR", new BigDecimal("-1")))));
    }
}
