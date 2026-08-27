package com.automotive.costing.configuration.service;

import com.automotive.costing.configuration.dto.ConfigurationDtos.CostingDefaultsRequest;
import com.automotive.costing.configuration.dto.ConfigurationDtos.CostingDefaultsResponse;
import com.automotive.costing.configuration.entity.TenantCostingDefaults;
import com.automotive.costing.configuration.repository.TenantCostingDefaultsRepository;
import com.automotive.costing.common.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * P3-07c Tenant costing defaults.
 * §11: fx_missing_policy fixed NOT_AVAILABLE; release_incomplete_policy fixed BLOCK (not customer-editable).
 */
@Service
@RequiredArgsConstructor
public class CostingDefaultsService {

    public static final String FX_MISSING_POLICY = "NOT_AVAILABLE";
    public static final String RELEASE_INCOMPLETE_POLICY = "BLOCK";

    private final TenantCostingDefaultsRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public CostingDefaultsResponse get(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return empty(null);
        }
        return repository.findByCustomerId(customerId)
                .map(this::toResponse)
                .orElseGet(() -> empty(customerId));
    }

    @Transactional
    public CostingDefaultsResponse upsert(String customerId, CostingDefaultsRequest request) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }
        TenantCostingDefaults row = repository.findByCustomerId(customerId).orElseGet(() -> {
            TenantCostingDefaults n = new TenantCostingDefaults();
            n.setDefaultsId(UUID.randomUUID().toString());
            n.setCustomerId(customerId);
            return n;
        });
        String oldCurrency = row.getDefaultReportingCurrency();
        BigDecimal oldScrap = row.getDefaultScrapPercent();
        BigDecimal oldOverhead = row.getDefaultOverheadPercent();

        if (request.defaultReportingCurrency() != null && !request.defaultReportingCurrency().isBlank()) {
            row.setDefaultReportingCurrency(request.defaultReportingCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (request.defaultScrapPercent() != null) {
            if (request.defaultScrapPercent().signum() < 0) {
                throw new IllegalArgumentException("defaultScrapPercent must be >= 0");
            }
            row.setDefaultScrapPercent(request.defaultScrapPercent());
        }
        if (request.defaultOverheadPercent() != null) {
            if (request.defaultOverheadPercent().signum() < 0) {
                throw new IllegalArgumentException("defaultOverheadPercent must be >= 0");
            }
            row.setDefaultOverheadPercent(request.defaultOverheadPercent());
        }
        if (request.notes() != null) {
            row.setNotes(request.notes().isBlank() ? null : request.notes().trim());
        }
        repository.save(row);

        auditService.recordUpdate(
                "TenantCostingDefaults",
                customerId,
                java.util.Map.of(
                        "defaultReportingCurrency", String.valueOf(oldCurrency),
                        "defaultScrapPercent", String.valueOf(oldScrap),
                        "defaultOverheadPercent", String.valueOf(oldOverhead)
                ),
                java.util.Map.of(
                        "defaultReportingCurrency", String.valueOf(row.getDefaultReportingCurrency()),
                        "defaultScrapPercent", String.valueOf(row.getDefaultScrapPercent()),
                        "defaultOverheadPercent", String.valueOf(row.getDefaultOverheadPercent())
                )
        );

        return toResponse(row);
    }

    private CostingDefaultsResponse empty(String customerId) {
        return new CostingDefaultsResponse(
                null, customerId, null, null, null,
                FX_MISSING_POLICY, RELEASE_INCOMPLETE_POLICY, null,
                List.of(
                        "No tenant defaults saved yet.",
                        "fxMissingPolicy is system-fixed: NOT_AVAILABLE",
                        "releaseIncompletePolicy is system-fixed: BLOCK"
                )
        );
    }

    private CostingDefaultsResponse toResponse(TenantCostingDefaults row) {
        return new CostingDefaultsResponse(
                row.getDefaultsId(),
                row.getCustomerId(),
                row.getDefaultReportingCurrency(),
                row.getDefaultScrapPercent(),
                row.getDefaultOverheadPercent(),
                FX_MISSING_POLICY,
                RELEASE_INCOMPLETE_POLICY,
                row.getNotes(),
                List.of(
                        "fxMissingPolicy is system-fixed: NOT_AVAILABLE (not customer-editable).",
                        "releaseIncompletePolicy is system-fixed: BLOCK (P3-01 §12 / §11 decision 5)."
                )
        );
    }
}
