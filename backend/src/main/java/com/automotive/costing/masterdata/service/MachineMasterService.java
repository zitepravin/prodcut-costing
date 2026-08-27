package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.RateBuildUpResponse;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.Response;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.SimulationRequest;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.SimulationResponse;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.UpdateRequest;
import com.automotive.costing.masterdata.entity.MachineMaster;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.mapper.MachineMasterMapper;
import com.automotive.costing.masterdata.repository.MachineMasterRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * OHD-01, OHD-04, OHD-05: machine hourly rate build-up, versioned, audited.
 * A machine cannot be used in process costing until it has >=1 Active version
 * (SRS 4.5.5 business rule) - enforced implicitly here since only ACTIVE-status
 * records are eligible in the search default and the rate build-up computation.
 */
@Service
@RequiredArgsConstructor
public class MachineMasterService {

    private static final String ENTITY_TYPE = "MachineMaster";
    private static final int SCALE = 6;

    private final MachineMasterRepository repository;
    private final MachineMasterMapper mapper;
    private final AuditService auditService;
    private final AccessScopeService accessScopeService;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional
    public Response create(CreateRequest request) {
        accessScopeService.assertAccessForCurrentUser(ScopeType.PLANT, request.plantId());
        String customerId = tenantAccessGuard.requireCustomerId();
        if (repository.existsByCustomerIdAndCode(customerId, request.code())) {
            throw new DuplicateResourceException(
                    "A machine master record with code '" + request.code() + "' already exists (MDM-01).");
        }
        MachineMaster entity = mapper.toEntity(request);
        entity.setRecordId(UUID.randomUUID().toString());
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setVersionNumber(1);
        entity.setRate(computeHourlyRate(entity));
        entity.setStatus(resolveInitialStatus(entity));

        MachineMaster saved = repository.save(entity);
        auditService.recordCreate(ENTITY_TYPE, saved.getRecordId(), mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public Response createNewVersion(String code, UpdateRequest request) {
        MachineMaster current = repository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(tenantAccessGuard.requireCustomerId(), code)
                .orElseThrow(() -> ResourceNotFoundException.of("MachineMaster", code));

        if (request.effectiveFrom() != null && request.effectiveFrom().isBefore(current.getEffectiveFrom())) {
            throw new BusinessRuleViolationException(
                    "New version effective date cannot be earlier than the current version's effective date.",
                    "SRS-4.1.5-effective-dating");
        }

        MachineMaster newVersion = copyForNewVersion(current);
        mapper.updateEntityFromRequest(request, newVersion);
        newVersion.setVersionNumber(current.getVersionNumber() + 1);
        newVersion.setRate(computeHourlyRate(newVersion));
        newVersion.setStatus(resolveInitialStatus(newVersion));

        MachineMaster saved = repository.save(newVersion);
        auditService.recordUpdate(ENTITY_TYPE, saved.getRecordId(),
                mapper.toResponse(current), mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Response getById(String recordId) {
        return mapper.toResponse(repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("MachineMaster", recordId)));
    }

    @Transactional(readOnly = true)
    public Page<Response> search(MasterDataStatus status, String plantId, String searchTerm, Pageable pageable) {
        var restricted = accessScopeService.restrictedValuesForCurrentUser(ScopeType.PLANT);
        if (restricted.isPresent() && plantId != null) {
            accessScopeService.assertAccessForCurrentUser(ScopeType.PLANT, plantId);
        }
        Page<Response> page = repository.search(status, plantId, searchTerm, pageable).map(mapper::toResponse);
        if (restricted.isPresent() && plantId == null) {
            var allowed = restricted.get();
            var filtered = page.getContent().stream()
                    .filter(r -> r.plantId() == null || allowed.contains(r.plantId()))
                    .toList();
            return new PageImpl<>(filtered, pageable, filtered.size());
        }
        return page;
    }

    /** OHD-01: exposes the individually-editable components alongside the composed rate. */
    @Transactional(readOnly = true)
    public RateBuildUpResponse getRateBuildUp(String recordId) {
        MachineMaster m = repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("MachineMaster", recordId));
        BigDecimal depreciationComponent = depreciationComponentPerHour(m);
        return new RateBuildUpResponse(
                m.getCode(), depreciationComponent, m.getPowerCostPerHour(),
                m.getMaintenanceCostPerHour(), m.getFloorSpaceCostPerHour(), m.getRate());
    }

    /**
     * V227 — Approve PENDING_APPROVAL machine version; close prior ACTIVE effective window.
     */
    @Transactional
    public Response approve(String recordId) {
        MachineMaster entity = repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("MachineMaster", recordId));
        if (entity.getStatus() != MasterDataStatus.PENDING_APPROVAL) {
            throw new BusinessRuleViolationException(
                    "Only PENDING_APPROVAL machine versions can be approved (current=" + entity.getStatus() + ")",
                    "MDM-APPROVE-01");
        }
        if (entity.getEffectiveTo() != null && entity.getEffectiveTo().isBefore(entity.getEffectiveFrom())) {
            throw new BusinessRuleViolationException(
                    "effectiveTo cannot be before effectiveFrom", "MDM-DATE-01");
        }
        for (MachineMaster prior : repository.findByCustomerIdAndCodeOrderByVersionNumberDesc(tenantAccessGuard.requireCustomerId(), entity.getCode())) {
            if (prior.getRecordId().equals(entity.getRecordId())) continue;
            if (prior.getStatus() != MasterDataStatus.ACTIVE) continue;
            if (prior.getEffectiveTo() == null
                    || !prior.getEffectiveTo().isBefore(entity.getEffectiveFrom())) {
                LocalDate closeTo = entity.getEffectiveFrom().minusDays(1);
                if (closeTo.isBefore(prior.getEffectiveFrom())) {
                    throw new BusinessRuleViolationException(
                            "New version effectiveFrom overlaps prior version window for code " + entity.getCode(),
                            "MDM-DATE-02");
                }
                prior.setEffectiveTo(closeTo);
                repository.save(prior);
            }
        }
        entity.setStatus(MasterDataStatus.ACTIVE);
        MachineMaster saved = repository.save(entity);
        auditService.recordTransition(ENTITY_TYPE, saved.getRecordId(),
                MasterDataStatus.PENDING_APPROVAL.name(), MasterDataStatus.ACTIVE.name());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Response findEffectiveAsOf(String code, LocalDate asOf) {
        MachineMaster m = repository.findEffectiveAsOf(code, asOf)
                .orElseThrow(() -> ResourceNotFoundException.of("MachineMaster", code + "@" + asOf));
        return mapper.toResponse(m);
    }

    @Transactional
    public Response deactivate(String recordId) {
        MachineMaster entity = repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("MachineMaster", recordId));
        MachineMaster before = copyForNewVersion(entity);
        entity.setStatus(MasterDataStatus.INACTIVE);
        MachineMaster saved = repository.save(entity);
        auditService.recordUpdate(ENTITY_TYPE, saved.getRecordId(),
                mapper.toResponse(before), mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /**
     * OHD-06: simulates the hourly-rate effect of a capacity-utilization or capital-cost
     * change WITHOUT persisting anything - builds an in-memory copy of the current record,
     * applies only the overrides supplied, and recomputes via the same
     * {@link #computeHourlyRate} logic the real create/new-version paths use, so the
     * simulation can never silently drift from the actual rate formula.
     */
    @Transactional(readOnly = true)
    public SimulationResponse simulate(String recordId, SimulationRequest request) {
        MachineMaster current = repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("MachineMaster", recordId));

        MachineMaster hypothetical = copyForNewVersion(current);
        if (request.depreciationLifeYears() != null) {
            hypothetical.setDepreciationLifeYears(request.depreciationLifeYears());
        }
        if (request.capitalCost() != null) {
            hypothetical.setCapitalCost(request.capitalCost());
        }
        if (request.annualOperatingHours() != null) {
            hypothetical.setAnnualOperatingHours(request.annualOperatingHours());
        }
        if (request.powerCostPerHour() != null) {
            hypothetical.setPowerCostPerHour(request.powerCostPerHour());
        }
        if (request.maintenanceCostPerHour() != null) {
            hypothetical.setMaintenanceCostPerHour(request.maintenanceCostPerHour());
        }
        if (request.floorSpaceCostPerHour() != null) {
            hypothetical.setFloorSpaceCostPerHour(request.floorSpaceCostPerHour());
        }

        BigDecimal simulatedRate = computeHourlyRate(hypothetical);
        BigDecimal depreciationComponent = depreciationComponentPerHour(hypothetical);
        RateBuildUpResponse simulatedBuildUp = new RateBuildUpResponse(
                current.getCode(), depreciationComponent, hypothetical.getPowerCostPerHour(),
                hypothetical.getMaintenanceCostPerHour(), hypothetical.getFloorSpaceCostPerHour(), simulatedRate);

        return new SimulationResponse(current.getCode(), current.getRate(), simulatedRate,
                simulatedRate.subtract(current.getRate()), simulatedBuildUp);
    }

    /**
     * OHD-01: machine hourly rate = (capital cost / depreciation life / annual operating hours)
     * + power cost/hr + maintenance cost/hr + floor space cost/hr.
     * Guards divide-by-zero per TDD Section 6.2 ("guard every division against zero/blank
     * denominators with an explicit validation error, never a silent divide-by-zero").
     */
    private BigDecimal computeHourlyRate(MachineMaster m) {
        BigDecimal depreciationComponent = depreciationComponentPerHour(m);
        return depreciationComponent
                .add(nz(m.getPowerCostPerHour()))
                .add(nz(m.getMaintenanceCostPerHour()))
                .add(nz(m.getFloorSpaceCostPerHour()))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal depreciationComponentPerHour(MachineMaster m) {
        if (m.getCapitalCost() == null) {
            return BigDecimal.ZERO;
        }
        if (isZeroOrNull(m.getDepreciationLifeYears())) {
            throw new BusinessRuleViolationException(
                    "depreciationLifeYears must be greater than zero to compute a machine hourly rate.",
                    "TDD-6.2-divide-by-zero-guard");
        }
        if (isZeroOrNull(m.getAnnualOperatingHours())) {
            throw new BusinessRuleViolationException(
                    "annualOperatingHours must be greater than zero to compute a machine hourly rate.",
                    "TDD-6.2-divide-by-zero-guard");
        }
        return m.getCapitalCost()
                .divide(m.getDepreciationLifeYears(), 10, RoundingMode.HALF_UP)
                .divide(m.getAnnualOperatingHours(), 10, RoundingMode.HALF_UP);
    }

    private boolean isZeroOrNull(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private MasterDataStatus resolveInitialStatus(MachineMaster entity) {
        boolean missingMandatory = entity.getRate() == null
                || entity.getUom() == null || entity.getUom().isBlank();
        return missingMandatory ? MasterDataStatus.PENDING_APPROVAL : MasterDataStatus.ACTIVE;
    }

    private MachineMaster copyForNewVersion(MachineMaster source) {
        MachineMaster copy = new MachineMaster();
        copy.setRecordId(UUID.randomUUID().toString());
        copy.setCode(source.getCode());
        copy.setDescription(source.getDescription());
        copy.setRate(source.getRate());
        copy.setUom(source.getUom());
        copy.setPlantId(source.getPlantId());
        copy.setEffectiveFrom(source.getEffectiveFrom());
        copy.setEffectiveTo(source.getEffectiveTo());
        copy.setMachineType(source.getMachineType());
        copy.setCapacity(source.getCapacity());
        copy.setDepreciationLifeYears(source.getDepreciationLifeYears());
        copy.setCapitalCost(source.getCapitalCost());
        copy.setAnnualOperatingHours(source.getAnnualOperatingHours());
        copy.setPowerCostPerHour(source.getPowerCostPerHour());
        copy.setMaintenanceCostPerHour(source.getMaintenanceCostPerHour());
        copy.setFloorSpaceCostPerHour(source.getFloorSpaceCostPerHour());
        copy.setShiftPattern(source.getShiftPattern());
        return copy;
    }
}
