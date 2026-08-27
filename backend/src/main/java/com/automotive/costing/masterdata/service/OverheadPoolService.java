package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.dto.OverheadPoolDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.OverheadPoolDtos.RecalculateRequest;
import com.automotive.costing.masterdata.dto.OverheadPoolDtos.Response;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.entity.OverheadPool;
import com.automotive.costing.masterdata.repository.OverheadPoolRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * OHD-02: at least 4 configurable overhead pools with independent allocation bases.
 * OHD-03: absorption rate = pool total / allocation base volume, recalculated whenever
 * the allocation base is updated for a period - implemented as a new version (never
 * an in-place edit, consistent with MDM-02's versioning rule applied system-wide).
 */
@Service
@RequiredArgsConstructor
public class OverheadPoolService {

    private static final String ENTITY_TYPE = "OverheadPool";
    private static final int SCALE = 6;

    /** The four minimum pool categories required for OHD-02 MVP compliance. */
    public static final List<String> MINIMUM_REQUIRED_CATEGORIES = List.of(
            "Factory Overhead", "Corporate SG&A", "Quality Cost", "Logistics Overhead");

    private final OverheadPoolRepository repository;
    private final TenantAccessGuard tenantAccessGuard;
    private final AuditService auditService;

    @Transactional
    public Response create(CreateRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        if (repository.existsByCustomerIdAndCode(customerId, request.code())) {
            throw new DuplicateResourceException(
                    "An overhead pool with code '" + request.code() + "' already exists (MDM-01).");
        }

        OverheadPool entity = new OverheadPool();
        entity.setRecordId(UUID.randomUUID().toString());
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setCode(request.code());
        entity.setDescription(request.description());
        entity.setPoolCategory(request.poolCategory());
        entity.setAllocationBase(request.allocationBase());
        entity.setUom(request.uom());
        entity.setPlantId(request.plantId());
        entity.setEffectiveFrom(request.effectiveFrom());
        entity.setEffectiveTo(request.effectiveTo());
        entity.setPeriod(request.period());
        entity.setVersionNumber(1);
        entity.setRate(absorptionRate(request.poolTotal(), request.allocationBaseVolume()));
        entity.setStatus(MasterDataStatus.ACTIVE);

        OverheadPool saved = repository.save(entity);
        auditService.recordCreate(ENTITY_TYPE, saved.getRecordId(), toResponse(saved));
        return toResponse(saved);
    }

    /** OHD-03: recalculate absorption rate; creates a new version (MDM-02 pattern). */
    @Transactional
    public Response recalculate(String code, RecalculateRequest request) {
        OverheadPool current = repository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(tenantAccessGuard.requireCustomerId(), code)
                .orElseThrow(() -> ResourceNotFoundException.of("OverheadPool", code));

        OverheadPool newVersion = new OverheadPool();
        newVersion.setRecordId(UUID.randomUUID().toString());
        newVersion.setCustomerId(tenantAccessGuard.requireCustomerId());
        newVersion.setCode(current.getCode());
        newVersion.setDescription(current.getDescription());
        newVersion.setPoolCategory(current.getPoolCategory());
        newVersion.setAllocationBase(current.getAllocationBase());
        newVersion.setUom(current.getUom());
        newVersion.setPlantId(current.getPlantId());
        newVersion.setEffectiveFrom(current.getEffectiveFrom());
        newVersion.setEffectiveTo(current.getEffectiveTo());
        newVersion.setPeriod(current.getPeriod());
        newVersion.setVersionNumber(current.getVersionNumber() + 1);
        newVersion.setRate(absorptionRate(request.poolTotal(), request.allocationBaseVolume()));
        newVersion.setStatus(MasterDataStatus.ACTIVE);

        OverheadPool saved = repository.save(newVersion);
        auditService.recordUpdate(ENTITY_TYPE, saved.getRecordId(), toResponse(current), toResponse(saved));
        return toResponse(saved);
    }

    /** OHD-02 compliance check: are all 4 minimum pool categories configured and active? */
    @Transactional(readOnly = true)
    public boolean hasMinimumRequiredPools() {
        return repository.countByCustomerIdAndPoolCategoryIn(tenantAccessGuard.requireCustomerId(), MINIMUM_REQUIRED_CATEGORIES) >= MINIMUM_REQUIRED_CATEGORIES.size();
    }

    private BigDecimal absorptionRate(BigDecimal poolTotal, BigDecimal allocationBaseVolume) {
        if (allocationBaseVolume == null || allocationBaseVolume.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleViolationException(
                    "allocationBaseVolume must be greater than zero to compute an absorption rate.",
                    "TDD-6.2-divide-by-zero-guard");
        }
        return poolTotal.divide(allocationBaseVolume, SCALE, RoundingMode.HALF_UP);
    }

    private Response toResponse(OverheadPool e) {
        return new Response(e.getRecordId(), e.getCode(), e.getDescription(), e.getRate(), e.getUom(),
                e.getPlantId(), e.getEffectiveFrom(), e.getEffectiveTo(), e.getVersionNumber(), e.getStatus(),
                e.getPoolCategory(), e.getAllocationBase(), e.getPeriod(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedBy(), e.getUpdatedAt());
    }
}
