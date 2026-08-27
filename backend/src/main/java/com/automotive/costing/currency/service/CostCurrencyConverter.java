package com.automotive.costing.currency.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertRequest;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CostCurrencyConverter {

    private static final Logger log = LoggerFactory.getLogger(CostCurrencyConverter.class);
    private final CurrencyService currencyService;

    @Value("${app.costing.base-currency:USD}")
    private String baseCurrency;

    public record FxConversionResult(
            BigDecimal originalAmount,
            String sourceCurrency,
            BigDecimal convertedAmount,
            String targetCurrency,
            BigDecimal appliedRate,
            LocalDate rateEffectiveDate,
            LocalDate asOfDate,
            String conversionMethod,
            String rateSource
    ) {
        public boolean isIdentity() {
            return "IDENTITY".equals(conversionMethod)
                    || (sourceCurrency != null && sourceCurrency.equalsIgnoreCase(targetCurrency));
        }
    }

    public BigDecimal toProgramCurrency(BigDecimal amount, String targetCurrency, LocalDate asOfDate) {
        return convertToProgramCurrency(amount, targetCurrency, asOfDate).convertedAmount();
    }

    public FxConversionResult convertToProgramCurrency(BigDecimal amount, String targetCurrency, LocalDate asOfDate) {
        if (amount == null) {
            return identityResult(BigDecimal.ZERO, normalize(baseCurrency), normalize(targetCurrency), asOfDate);
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            String base = normalize(baseCurrency);
            String target = normalize(targetCurrency);
            if (target == null) target = base;
            return identityResult(amount, base, target, asOfDate);
        }
        String target = normalize(targetCurrency);
        String base = normalize(baseCurrency);
        if (target == null || target.equals(base)) {
            return identityResult(amount, base, base, asOfDate);
        }
        if (asOfDate == null) {
            throw new BusinessRuleViolationException(
                    "Costing conversion requires an explicit asOfDate / costing date",
                    "CUR-ASOF-REQUIRED");
        }
        ConvertResponse response = currencyService.convert(new ConvertRequest(amount, base, target, asOfDate));
        log.debug("FX {} {} -> {} {} path={} rate={} source={}", amount, base, response.convertedAmount(),
                target, response.conversionPath(), response.rateUsed(), response.rateSource());
        return new FxConversionResult(
                response.originalAmount(), response.fromCurrency(), response.convertedAmount(),
                response.toCurrency(), response.rateUsed(), response.rateEffectiveDate(),
                asOfDate, response.conversionPath(), response.rateSource());
    }

    @Deprecated
    public BigDecimal toProgramCurrency(BigDecimal amount, String targetCurrency) {
        String target = normalize(targetCurrency);
        String base = normalize(baseCurrency);
        if (amount == null) return BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) == 0) return amount;
        if (target == null || target.equals(base)) return amount;
        throw new BusinessRuleViolationException(
                "Cross-currency conversion requires explicit asOfDate", "CUR-ASOF-REQUIRED");
    }

    public String getBaseCurrency() { return normalize(baseCurrency); }

    private static FxConversionResult identityResult(BigDecimal amount, String source, String target, LocalDate asOfDate) {
        return new FxConversionResult(amount, source, amount, target != null ? target : source,
                BigDecimal.ONE, asOfDate, asOfDate, "IDENTITY", null);
    }

    private static String normalize(String code) {
        if (code == null || code.isBlank()) return null;
        return code.trim().toUpperCase();
    }
}
