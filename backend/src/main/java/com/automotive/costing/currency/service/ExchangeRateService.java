package com.automotive.costing.currency.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.currency.dto.CurrencyDtos.*;
import com.automotive.costing.currency.entity.ExchangeRate;
import com.automotive.costing.currency.repository.ExchangeRateRepository;
import com.automotive.costing.masterdata.entity.Currency;
import com.automotive.costing.masterdata.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String ENTITY_TYPE = "ExchangeRate";

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;
    private final AuditService auditService;

    @Transactional
    public ExchangeRateResponse publish(PublishRateRequest request) {
        String from = request.fromCurrency().trim().toUpperCase();
        String to = request.toCurrency().trim().toUpperCase();

        if (from.equals(to)) {
            throw new BusinessRuleViolationException(
                    "fromCurrency and toCurrency must differ", "CUR-05");
        }
        requireCurrency(from);
        requireCurrency(to);

        if (request.effectiveDate().isAfter(LocalDate.now().plusDays(30))) {
            throw new BusinessRuleViolationException(
                    "effectiveDate cannot be more than 30 days in the future", "CUR-06");
        }

        ExchangeRate rate = new ExchangeRate();
        rate.setRateId(UUID.randomUUID().toString());
        rate.setFromCurrency(from);
        rate.setToCurrency(to);
        rate.setRate(request.rate());
        rate.setEffectiveDate(request.effectiveDate());
        rate.setSource(request.source() != null ? request.source() : "MANUAL");

        ExchangeRate saved = exchangeRateRepository.save(rate);
        auditService.recordCreate(ENTITY_TYPE, saved.getRateId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RateHistoryResponse history(String fromCurrency, String toCurrency) {
        String from = fromCurrency.trim().toUpperCase();
        String to = toCurrency.trim().toUpperCase();
        List<ExchangeRateResponse> rates = exchangeRateRepository
                .findByFromCurrencyAndToCurrencyOrderByEffectiveDateDesc(from, to)
                .stream()
                .map(this::toResponse)
                .toList();
        return new RateHistoryResponse(from, to, rates);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> recentRates(LocalDate since) {
        return exchangeRateRepository
                .findByEffectiveDateGreaterThanEqualOrderByEffectiveDateDesc(since)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void requireCurrency(String code) {
        currencyRepository.findById(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Currency", code));
    }

    private ExchangeRateResponse toResponse(ExchangeRate e) {
        return new ExchangeRateResponse(
                e.getRateId(),
                e.getFromCurrency(),
                e.getToCurrency(),
                e.getRate(),
                e.getEffectiveDate(),
                e.getSource(),
                e.getCreatedAt());
    }
}
