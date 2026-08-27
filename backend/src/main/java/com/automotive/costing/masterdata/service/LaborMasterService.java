package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.dto.LaborMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.LaborMasterDtos.Response;
import com.automotive.costing.masterdata.dto.LaborMasterDtos.UpdateRequest;
import com.automotive.costing.masterdata.entity.LaborMaster;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.mapper.LaborMasterMapper;
import com.automotive.costing.masterdata.repository.LaborMasterRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LaborMasterService {

    private static final String ENTITY_TYPE = "LaborMaster";

    private final LaborMasterRepository repository;
    private final LaborMasterMapper mapper;
    private final AuditService auditService;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional
    public Response create(CreateRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        if (repository.existsByCustomerIdAndCode(customerId, request.code())) {
            throw new DuplicateResourceException(
                    "A labor master record with code '" + request.code() + "' already exists (MDM-01).");
        }
        LaborMaster entity = mapper.toEntity(request);
        entity.setRecordId(UUID.randomUUID().toString());
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setVersionNumber(1);
        entity.setStatus(resolveInitialStatus(entity));

        LaborMaster saved = repository.save(entity);
        auditService.recordCreate(ENTITY_TYPE, saved.getRecordId(), mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public Response createNewVersion(String code, UpdateRequest request) {
        LaborMaster current = repository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(tenantAccessGuard.requireCustomerId(), code)
                .orElseThrow(() -> ResourceNotFoundException.of("LaborMaster", code));

        if (request.effectiveFrom() != null && request.effectiveFrom().isBefore(current.getEffectiveFrom())) {
            throw new BusinessRuleViolationException(
                    "New version effective date cannot be earlier than the current version's effective date.",
                    "SRS-4.1.5-effective-dating");
        }

        LaborMaster newVersion = copyForNewVersion(current);
        mapper.updateEntityFromRequest(request, newVersion);
        newVersion.setVersionNumber(current.getVersionNumber() + 1);
        newVersion.setStatus(resolveInitialStatus(newVersion));

        LaborMaster saved = repository.save(newVersion);
        auditService.recordUpdate(ENTITY_TYPE, saved.getRecordId(),
                mapper.toResponse(current), mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Response getById(String recordId) {
        return mapper.toResponse(repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("LaborMaster", recordId)));
    }

    @Transactional(readOnly = true)
    public Page<Response> search(MasterDataStatus status, String plantId, String searchTerm, Pageable pageable) {
        return repository.search(status, plantId, searchTerm, pageable).map(mapper::toResponse);
    }

    /**
     * V227 — Approve PENDING_APPROVAL labor version; close prior ACTIVE effective window.
     */
    @Transactional
    public Response approve(String recordId) {
        LaborMaster entity = repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("LaborMaster", recordId));
        if (entity.getStatus() != MasterDataStatus.PENDING_APPROVAL) {
            throw new BusinessRuleViolationException(
                    "Only PENDING_APPROVAL labor versions can be approved (current=" + entity.getStatus() + ")",
                    "MDM-APPROVE-01");
        }
        if (entity.getEffectiveTo() != null && entity.getEffectiveTo().isBefore(entity.getEffectiveFrom())) {
            throw new BusinessRuleViolationException(
                    "effectiveTo cannot be before effectiveFrom", "MDM-DATE-01");
        }
        for (LaborMaster prior : repository.findByCustomerIdAndCodeOrderByVersionNumberDesc(tenantAccessGuard.requireCustomerId(), entity.getCode())) {
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
        LaborMaster saved = repository.save(entity);
        auditService.recordTransition(ENTITY_TYPE, saved.getRecordId(),
                MasterDataStatus.PENDING_APPROVAL.name(), MasterDataStatus.ACTIVE.name());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Response findEffectiveAsOf(String code, LocalDate asOf) {
        LaborMaster m = repository.findEffectiveAsOf(code, asOf)
                .orElseThrow(() -> ResourceNotFoundException.of("LaborMaster", code + "@" + asOf));
        return mapper.toResponse(m);
    }

    @Transactional
    public Response deactivate(String recordId) {
        LaborMaster entity = repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("LaborMaster", recordId));
        LaborMaster before = copyForNewVersion(entity);
        entity.setStatus(MasterDataStatus.INACTIVE);
        LaborMaster saved = repository.save(entity);
        auditService.recordUpdate(ENTITY_TYPE, saved.getRecordId(),
                mapper.toResponse(before), mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    private MasterDataStatus resolveInitialStatus(LaborMaster entity) {
        boolean missingMandatory = entity.getRate() == null
                || entity.getUom() == null || entity.getUom().isBlank();
        return missingMandatory ? MasterDataStatus.PENDING_APPROVAL : MasterDataStatus.ACTIVE;
    }

    private LaborMaster copyForNewVersion(LaborMaster source) {
        LaborMaster copy = new LaborMaster();
        copy.setRecordId(UUID.randomUUID().toString());
        copy.setCode(source.getCode());
        copy.setDescription(source.getDescription());
        copy.setRate(source.getRate());
        copy.setUom(source.getUom());
        copy.setPlantId(source.getPlantId());
        copy.setEffectiveFrom(source.getEffectiveFrom());
        copy.setEffectiveTo(source.getEffectiveTo());
        copy.setOperatorGrade(source.getOperatorGrade());
        copy.setShiftType(source.getShiftType());
        copy.setCountryRegion(source.getCountryRegion());
        copy.setStatutoryBurdenPct(source.getStatutoryBurdenPct());
        return copy;
    }
}
