package com.automotive.costing.currency.service;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.currency.dto.CurrencyDtos.*;
import com.automotive.costing.currency.engine.CurrencyConversionEngine;
import com.automotive.costing.currency.engine.CurrencyConversionEngine.ConversionResult;
import com.automotive.costing.currency.entity.ExchangeRate;
import com.automotive.costing.currency.repository.ExchangeRateRepository;
import com.automotive.costing.masterdata.entity.Currency;
import com.automotive.costing.masterdata.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional(readOnly = true)
    public List<CurrencyResponse> listCurrencies() {
        return currencyRepository.findAll().stream()
                .map(c -> new CurrencyResponse(c.getCurrencyCode(), c.getDescription(), c.getDecimalPlaces()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CurrencyResponse getCurrency(String code) {
        Currency c = currencyRepository.findById(code.trim().toUpperCase())
                .orElseThrow(() -> ResourceNotFoundException.of("Currency", code));
        return new CurrencyResponse(c.getCurrencyCode(), c.getDescription(), c.getDecimalPlaces());
    }

    /**
     * Convert using most recent rate on or before asOfDate.
     * Captures real {@link ExchangeRate#getSource()} from the rate row used.
     */
    @Transactional(readOnly = true)
    public ConvertResponse convert(ConvertRequest request) {
        LocalDate asOf = request.asOfDate() != null ? request.asOfDate() : LocalDate.now();
        String from = request.fromCurrency().trim().toUpperCase();
        String to = request.toCurrency().trim().toUpperCase();

        final LocalDate[] rateDateHolder = { asOf };
        final String[] rateSourceHolder = { null };

        ConversionResult result = CurrencyConversionEngine.convert(
                request.amount(),
                from,
                to,
                (f, t) -> {
                    Optional<ExchangeRate> erOpt = exchangeRateRepository.findEffectiveRate(f, t, asOf);
                    if (erOpt.isPresent()) {
                        ExchangeRate er = erOpt.get();
                        rateDateHolder[0] = er.getEffectiveDate();
                        if (rateSourceHolder[0] == null && er.getSource() != null && !er.getSource().isBlank()) {
                            rateSourceHolder[0] = er.getSource();
                        }
                        return Optional.of(er.getRate());
                    }
                    return Optional.empty();
                });

        if ("IDENTITY".equals(result.conversionPath())) {
            rateSourceHolder[0] = null;
            rateDateHolder[0] = asOf;
        }

        return new ConvertResponse(
                request.amount(),
                from,
                result.convertedAmount(),
                to,
                result.rateUsed(),
                rateDateHolder[0],
                result.conversionPath(),
                rateSourceHolder[0]);
    }
}
