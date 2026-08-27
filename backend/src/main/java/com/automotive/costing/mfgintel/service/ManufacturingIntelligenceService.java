package com.automotive.costing.mfgintel.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.mfgintel.dto.MfgIntelDtos.*;
import com.automotive.costing.mfgintel.engine.CapacityUtilizationEngine;
import com.automotive.costing.mfgintel.engine.CapacityUtilizationEngine.UtilizationResult;
import com.automotive.costing.mfgintel.entity.ActivityType;
import com.automotive.costing.mfgintel.entity.CapacityCenter;
import com.automotive.costing.mfgintel.entity.CapacityLoad;
import com.automotive.costing.mfgintel.repository.ActivityTypeRepository;
import com.automotive.costing.mfgintel.repository.CapacityCenterRepository;
import com.automotive.costing.mfgintel.repository.CapacityLoadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * D1 Activity-based costing + D2 Capacity / utilization.
 * Does not invent rates or available hours.
 */
@Service
@RequiredArgsConstructor
public class ManufacturingIntelligenceService {

    private static final int SCALE = 6;

    private final ActivityTypeRepository activityTypeRepository;
    private final CapacityCenterRepository capacityCenterRepository;
    private final CapacityLoadRepository capacityLoadRepository;

    // --- D1 Activity types ---

    @Transactional(readOnly = true)
    public List<ActivityTypeResponse> listActivities() {
        return activityTypeRepository.findByActiveTrueOrderByActivityCodeAsc().stream()
                .map(this::toActivity).toList();
    }

    @Transactional
    public ActivityTypeResponse createActivity(CreateActivityTypeRequest req) {
        activityTypeRepository.findByActivityCode(req.activityCode().trim()).ifPresent(a -> {
            throw new BusinessRuleViolationException("Activity code exists", "ACT_CODE_EXISTS");
        });
        ActivityType a = new ActivityType();
        a.setActivityTypeId(UUID.randomUUID().toString());
        a.setActivityCode(req.activityCode().trim());
        a.setActivityName(req.activityName().trim());
        a.setCategory(req.category() == null || req.category().isBlank() ? "MACHINE" : req.category().trim().toUpperCase());
        a.setUnitOfMeasure(req.unitOfMeasure() == null || req.unitOfMeasure().isBlank() ? "HR" : req.unitOfMeasure().trim());
        a.setDefaultRate(req.defaultRate());
        a.setCurrencyCode(req.currencyCode() == null || req.currencyCode().isBlank() ? "USD" : req.currencyCode().trim().toUpperCase());
        a.setCostCenterCode(trimToNull(req.costCenterCode()));
        a.setNotes(trimToNull(req.notes()));
        a.setActive(true);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        return toActivity(activityTypeRepository.save(a));
    }

    @Transactional(readOnly = true)
    public ActivityCostResponse calculateActivityCost(ActivityCostRequest req) {
        ActivityType a = activityTypeRepository.findByActivityCode(req.activityCode().trim())
                .orElseThrow(() -> ResourceNotFoundException.of("ActivityType", req.activityCode()));
        if (!a.isActive()) {
            throw new BusinessRuleViolationException("Activity is inactive", "ACT_INACTIVE");
        }
        BigDecimal rate = req.rateOverride() != null ? req.rateOverride() : a.getDefaultRate();
        if (rate == null) {
            throw new BusinessRuleViolationException(
                    "No rate defined for activity " + a.getActivityCode() + " — provide rateOverride or defaultRate",
                    "ACT_NO_RATE");
        }
        if (req.quantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("Quantity cannot be negative", "ACT_QTY");
        }
        BigDecimal cost = CapacityUtilizationEngine.activityCost(req.quantity(), rate);
        return new ActivityCostResponse(
                a.getActivityCode(), a.getActivityName(), a.getCategory(),
                req.quantity().setScale(SCALE, RoundingMode.HALF_UP),
                a.getUnitOfMeasure(), rate.setScale(SCALE, RoundingMode.HALF_UP),
                cost, a.getCurrencyCode(),
                "qty(" + req.quantity().toPlainString() + ") x rate(" + rate.toPlainString() + ")"
        );
    }

    // --- D2 Capacity ---

    @Transactional(readOnly = true)
    public List<CapacityCenterResponse> listCenters(String periodCode) {
        List<CapacityCenter> list = periodCode == null || periodCode.isBlank()
                ? capacityCenterRepository.findByActiveTrueOrderByCenterCodeAsc()
                : capacityCenterRepository.findByPeriodCodeAndActiveTrueOrderByCenterCodeAsc(periodCode.trim());
        return list.stream().map(this::toCenter).toList();
    }

    @Transactional
    public CapacityCenterResponse createCenter(CreateCapacityCenterRequest req) {
        capacityCenterRepository.findByCenterCodeAndPeriodCode(req.centerCode().trim(), req.periodCode().trim())
                .ifPresent(c -> {
                    throw new BusinessRuleViolationException("Center+period exists", "CAP_EXISTS");
                });
        if (req.availableHours().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("availableHours cannot be negative", "CAP_HOURS");
        }
        CapacityCenter c = new CapacityCenter();
        c.setCapacityCenterId(UUID.randomUUID().toString());
        c.setCenterCode(req.centerCode().trim());
        c.setCenterName(req.centerName().trim());
        c.setPlantCode(trimToNull(req.plantCode()));
        c.setAvailableHours(req.availableHours().setScale(SCALE, RoundingMode.HALF_UP));
        c.setPeriodCode(req.periodCode().trim());
        c.setEfficiencyPct(req.efficiencyPct() == null ? BigDecimal.ONE
                : req.efficiencyPct().setScale(SCALE, RoundingMode.HALF_UP));
        c.setActive(true);
        c.setNotes(trimToNull(req.notes()));
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return toCenter(capacityCenterRepository.save(c));
    }

    @Transactional
    public LoadResponse addLoad(String capacityCenterId, AddLoadRequest req) {
        CapacityCenter c = requireCenter(capacityCenterId);
        if (req.requiredHours().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("requiredHours cannot be negative", "CAP_LOAD");
        }
        CapacityLoad load = new CapacityLoad();
        load.setLoadId(UUID.randomUUID().toString());
        load.setCapacityCenterId(c.getCapacityCenterId());
        load.setActivityTypeId(trimToNull(req.activityTypeId()));
        load.setSourceRefType(trimToNull(req.sourceRefType()));
        load.setSourceRefId(trimToNull(req.sourceRefId()));
        load.setRequiredHours(req.requiredHours().setScale(SCALE, RoundingMode.HALF_UP));
        load.setLoadLabel(trimToNull(req.loadLabel()));
        load.setCreatedAt(Instant.now());
        CapacityLoad saved = capacityLoadRepository.save(load);
        return new LoadResponse(saved.getLoadId(), saved.getRequiredHours(), saved.getActivityTypeId(),
                saved.getSourceRefType(), saved.getSourceRefId(), saved.getLoadLabel());
    }

    @Transactional(readOnly = true)
    public UtilizationResponse utilization(String capacityCenterId) {
        CapacityCenter c = requireCenter(capacityCenterId);
        List<CapacityLoad> loads = capacityLoadRepository.findByCapacityCenterIdOrderByCreatedAtAsc(capacityCenterId);
        UtilizationResult r = CapacityUtilizationEngine.calculate(
                c.getAvailableHours(),
                c.getEfficiencyPct(),
                loads.stream().map(CapacityLoad::getRequiredHours).toList()
        );
        List<LoadResponse> loadResponses = loads.stream()
                .map(l -> new LoadResponse(l.getLoadId(), l.getRequiredHours(), l.getActivityTypeId(),
                        l.getSourceRefType(), l.getSourceRefId(), l.getLoadLabel()))
                .toList();
        return new UtilizationResponse(
                c.getCapacityCenterId(), c.getCenterCode(), c.getPeriodCode(),
                r.availableHours(), r.efficiencyPct(), r.effectiveHours(),
                r.requiredHours(), r.residualHours(), r.utilizationPct(),
                r.status(), loadResponses
        );
    }

    private CapacityCenter requireCenter(String id) {
        return capacityCenterRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CapacityCenter", id));
    }

    private ActivityTypeResponse toActivity(ActivityType a) {
        return new ActivityTypeResponse(
                a.getActivityTypeId(), a.getActivityCode(), a.getActivityName(), a.getCategory(),
                a.getUnitOfMeasure(), a.getDefaultRate(), a.getCurrencyCode(),
                a.getCostCenterCode(), a.isActive(), a.getNotes()
        );
    }

    private CapacityCenterResponse toCenter(CapacityCenter c) {
        return new CapacityCenterResponse(
                c.getCapacityCenterId(), c.getCenterCode(), c.getCenterName(), c.getPlantCode(),
                c.getAvailableHours(), c.getPeriodCode(), c.getEfficiencyPct(), c.isActive()
        );
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
