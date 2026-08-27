package com.automotive.costing.costingvariant.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.costingvariant.dto.CostingVariantDtos.*;
import com.automotive.costing.costingvariant.entity.*;
import com.automotive.costing.costingvariant.repository.CostingVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A1 Costing Variant Engine — configuration source of truth for estimate rules.
 * Does not calculate costs itself; consumers (should-cost, future mass run) apply these rules.
 */
@Service
@RequiredArgsConstructor
public class CostingVariantService {

    private static final int MONEY_SCALE = 6;

    private final CostingVariantRepository repository;

    @Transactional(readOnly = true)
    public List<VariantResponse> list() {
        return repository.findAllByOrderByVariantCodeAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> listActive() {
        return repository.findByStatusOrderByVariantCodeAsc(CostingVariantStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VariantResponse get(String variantId) {
        return toResponse(require(variantId));
    }

    @Transactional(readOnly = true)
    public CostingVariant requireActiveForCosting(String variantId) {
        CostingVariant v = require(variantId);
        if (v.getStatus() != CostingVariantStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Costing variant must be ACTIVE to use in an estimate: " + v.getVariantCode(),
                    "CV_NOT_ACTIVE");
        }
        LocalDate today = LocalDate.now();
        if (v.getValidFrom() != null && today.isBefore(v.getValidFrom())) {
            throw new BusinessRuleViolationException(
                    "Costing variant not yet valid: " + v.getVariantCode(), "CV_NOT_YET_VALID");
        }
        if (v.getValidTo() != null && today.isAfter(v.getValidTo())) {
            throw new BusinessRuleViolationException(
                    "Costing variant expired: " + v.getVariantCode(), "CV_EXPIRED");
        }
        return v;
    }

    @Transactional
    public VariantResponse create(CreateVariantRequest req) {
        String code = req.variantCode().trim();
        repository.findByVariantCode(code).ifPresent(existing -> {
            throw new BusinessRuleViolationException(
                    "Variant code already exists: " + code, "CV_CODE_EXISTS");
        });
        validatePcts(req.scrapPct(), req.yieldPct());
        validateLotSize(req.lotSize());

        CostingVariant v = new CostingVariant();
        v.setVariantId(UUID.randomUUID().toString());
        v.setVariantCode(code);
        v.setVariantName(req.variantName().trim());
        v.setStatus(CostingVariantStatus.DRAFT);
        v.setValuationStrategy(nz(req.valuationStrategy(), ValuationStrategy.STANDARD));
        v.setMaterialPriceSource(nz(req.materialPriceSource(), PriceSource.MATERIAL_MASTER));
        v.setActivityPriceSource(nz(req.activityPriceSource(), PriceSource.MACHINE_LABOR_MASTER));
        v.setQuantityStructureSource(nz(req.quantityStructureSource(), QuantityStructureSource.BOM_AND_ROUTING));
        v.setCurrencyCode(req.currencyCode() == null || req.currencyCode().isBlank()
                ? "USD" : req.currencyCode().trim().toUpperCase());
        v.setLotSize(scale(req.lotSize() != null ? req.lotSize() : BigDecimal.ONE));
        v.setScrapPct(scale(req.scrapPct() != null ? req.scrapPct() : BigDecimal.ZERO));
        v.setYieldPct(scale(req.yieldPct() != null ? req.yieldPct() : BigDecimal.ONE));
        v.setOverheadPoolCode(trimToNull(req.overheadPoolCode()));
        v.setCostComponentProfile(trimToNull(req.costComponentProfile()));
        v.setCostingSheetCode(trimToNull(req.costingSheetCode()));
        v.setValidFrom(req.validFrom());
        v.setValidTo(req.validTo());
        v.setDescription(trimToNull(req.description()));
        return toResponse(repository.save(v));
    }

    @Transactional
    public VariantResponse update(String variantId, UpdateVariantRequest req) {
        CostingVariant v = require(variantId);
        if (v.getStatus() == CostingVariantStatus.RETIRED) {
            throw new BusinessRuleViolationException("Cannot update RETIRED variant", "CV_RETIRED");
        }
        validatePcts(req.scrapPct(), req.yieldPct());
        validateLotSize(req.lotSize());
        if (req.variantName() != null && !req.variantName().isBlank()) {
            v.setVariantName(req.variantName().trim());
        }
        if (req.valuationStrategy() != null) v.setValuationStrategy(req.valuationStrategy());
        if (req.materialPriceSource() != null) v.setMaterialPriceSource(req.materialPriceSource());
        if (req.activityPriceSource() != null) v.setActivityPriceSource(req.activityPriceSource());
        if (req.quantityStructureSource() != null) v.setQuantityStructureSource(req.quantityStructureSource());
        if (req.currencyCode() != null && !req.currencyCode().isBlank()) {
            v.setCurrencyCode(req.currencyCode().trim().toUpperCase());
        }
        if (req.lotSize() != null) v.setLotSize(scale(req.lotSize()));
        if (req.scrapPct() != null) v.setScrapPct(scale(req.scrapPct()));
        if (req.yieldPct() != null) v.setYieldPct(scale(req.yieldPct()));
        if (req.overheadPoolCode() != null) v.setOverheadPoolCode(trimToNull(req.overheadPoolCode()));
        if (req.costComponentProfile() != null) v.setCostComponentProfile(trimToNull(req.costComponentProfile()));
        if (req.costingSheetCode() != null) v.setCostingSheetCode(trimToNull(req.costingSheetCode()));
        if (req.validFrom() != null) v.setValidFrom(req.validFrom());
        if (req.validTo() != null) v.setValidTo(req.validTo());
        if (req.description() != null) v.setDescription(trimToNull(req.description()));
        return toResponse(repository.save(v));
    }

    @Transactional
    public VariantResponse activate(String variantId) {
        CostingVariant v = require(variantId);
        if (v.getStatus() == CostingVariantStatus.RETIRED) {
            throw new BusinessRuleViolationException("Cannot activate RETIRED variant", "CV_RETIRED");
        }
        v.setStatus(CostingVariantStatus.ACTIVE);
        return toResponse(repository.save(v));
    }

    @Transactional
    public VariantResponse retire(String variantId) {
        CostingVariant v = require(variantId);
        v.setStatus(CostingVariantStatus.RETIRED);
        return toResponse(repository.save(v));
    }

    private CostingVariant require(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CostingVariant", id));
    }

    private VariantResponse toResponse(CostingVariant v) {
        return new VariantResponse(
                v.getVariantId(), v.getVariantCode(), v.getVariantName(), v.getStatus(),
                v.getValuationStrategy(), v.getMaterialPriceSource(), v.getActivityPriceSource(),
                v.getQuantityStructureSource(), v.getCurrencyCode(), v.getLotSize(),
                v.getScrapPct(), v.getYieldPct(), v.getOverheadPoolCode(),
                v.getCostComponentProfile(), v.getCostingSheetCode(),
                v.getValidFrom(), v.getValidTo(), v.getDescription(), v.getUpdatedAt()
        );
    }

    private static void validateLotSize(BigDecimal lot) {
        if (lot != null && lot.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Lot size must be > 0", "CV_LOT_SIZE");
        }
    }

    private static void validatePcts(BigDecimal scrap, BigDecimal yield) {
        if (scrap != null && (scrap.compareTo(BigDecimal.ZERO) < 0 || scrap.compareTo(BigDecimal.ONE) > 0)) {
            throw new BusinessRuleViolationException("scrap_pct must be between 0 and 1", "CV_SCRAP_PCT");
        }
        if (yield != null && (yield.compareTo(BigDecimal.ZERO) <= 0 || yield.compareTo(BigDecimal.ONE) > 0)) {
            throw new BusinessRuleViolationException("yield_pct must be > 0 and <= 1", "CV_YIELD_PCT");
        }
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static <T> T nz(T v, T def) {
        return v != null ? v : def;
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
