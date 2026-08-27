package com.automotive.costing.financial.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertRequest;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertResponse;
import com.automotive.costing.currency.service.CurrencyService;
import com.automotive.costing.financial.dto.FinancialDtos.*;
import com.automotive.costing.financial.entity.ValuationCurrencyPolicy;
import com.automotive.costing.financial.repository.ValuationCurrencyPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * C4 — Company / group / transaction / costing currency matrix + valuation date policy.
 * Uses existing CurrencyService rates; does not invent FX.
 */
@Service
@RequiredArgsConstructor
public class ValuationCurrencyService {

    private final ValuationCurrencyPolicyRepository policyRepository;
    private final CurrencyService currencyService;

    @Transactional(readOnly = true)
    public List<ValuationPolicyResponse> listActive() {
        return policyRepository.findByActiveTrueOrderByPolicyCodeAsc().stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ValuationPolicyResponse create(CreateValuationPolicyRequest req) {
        policyRepository.findByPolicyCode(req.policyCode().trim()).ifPresent(p -> {
            throw new BusinessRuleViolationException("Policy code exists", "VCP_EXISTS");
        });
        String datePolicy = req.valuationDatePolicy() == null || req.valuationDatePolicy().isBlank()
                ? "COSTING_DATE" : req.valuationDatePolicy().trim().toUpperCase();
        if ("FIXED_DATE".equals(datePolicy) && req.fixedValuationDate() == null) {
            throw new BusinessRuleViolationException(
                    "FIXED_DATE policy requires fixedValuationDate", "VCP_FIXED_DATE");
        }

        ValuationCurrencyPolicy p = new ValuationCurrencyPolicy();
        p.setPolicyId(UUID.randomUUID().toString());
        p.setPolicyCode(req.policyCode().trim());
        p.setPolicyName(req.policyName().trim());
        p.setCompanyCurrency(req.companyCurrency().trim().toUpperCase());
        p.setGroupCurrency(trimUpper(req.groupCurrency()));
        p.setTransactionCurrency(trimUpper(req.transactionCurrency()));
        p.setCostingCurrency(req.costingCurrency().trim().toUpperCase());
        p.setValuationDatePolicy(datePolicy);
        p.setFixedValuationDate(req.fixedValuationDate());
        p.setActive(true);
        p.setNotes(req.notes());
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return toResponse(policyRepository.save(p));
    }

    @Transactional(readOnly = true)
    public ValuationConvertResponse convert(ValuationConvertRequest req) {
        ValuationCurrencyPolicy p = policyRepository.findByPolicyCode(req.policyCode().trim())
                .orElseThrow(() -> ResourceNotFoundException.of("ValuationCurrencyPolicy", req.policyCode()));
        if (!p.isActive()) {
            throw new BusinessRuleViolationException("Policy is inactive", "VCP_INACTIVE");
        }

        LocalDate valuationDate = resolveValuationDate(p, req.costingDate(), req.documentDate());
        String from = req.fromCurrency().trim().toUpperCase();

        BigDecimal company = convertAmount(req.amount(), from, p.getCompanyCurrency(), valuationDate);
        BigDecimal group = p.getGroupCurrency() != null
                ? convertAmount(req.amount(), from, p.getGroupCurrency(), valuationDate) : null;
        BigDecimal costing = convertAmount(req.amount(), from, p.getCostingCurrency(), valuationDate);

        return new ValuationConvertResponse(
                p.getPolicyCode(),
                req.amount(),
                from,
                company,
                p.getCompanyCurrency(),
                group,
                p.getGroupCurrency(),
                costing,
                p.getCostingCurrency(),
                valuationDate,
                "Converted using FX rates as of " + valuationDate + " via CurrencyService"
        );
    }

    private LocalDate resolveValuationDate(ValuationCurrencyPolicy p, LocalDate costingDate, LocalDate documentDate) {
        return switch (p.getValuationDatePolicy()) {
            case "DOCUMENT_DATE" -> documentDate != null ? documentDate : LocalDate.now();
            case "TODAY" -> LocalDate.now();
            case "FIXED_DATE" -> p.getFixedValuationDate() != null ? p.getFixedValuationDate() : LocalDate.now();
            default -> costingDate != null ? costingDate : LocalDate.now(); // COSTING_DATE
        };
    }

    private BigDecimal convertAmount(BigDecimal amount, String from, String to, LocalDate asOf) {
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }
        ConvertResponse r = currencyService.convert(new ConvertRequest(amount, from, to, asOf));
        return r.convertedAmount();
    }

    private ValuationPolicyResponse toResponse(ValuationCurrencyPolicy p) {
        return new ValuationPolicyResponse(
                p.getPolicyId(), p.getPolicyCode(), p.getPolicyName(),
                p.getCompanyCurrency(), p.getGroupCurrency(), p.getTransactionCurrency(),
                p.getCostingCurrency(), p.getValuationDatePolicy(), p.getFixedValuationDate(),
                p.isActive(), p.getNotes()
        );
    }

    private static String trimUpper(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim().toUpperCase();
    }
}
