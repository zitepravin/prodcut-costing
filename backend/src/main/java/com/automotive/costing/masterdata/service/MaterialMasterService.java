package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.common.event.MaterialRatePublishedEvent;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.Response;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.UpdateRequest;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.entity.MaterialMaster;
import com.automotive.costing.masterdata.mapper.MaterialMasterMapper;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for Material Master (M01, MDM-01..MDM-08 + module business rules).
 * Controllers contain no logic - every rule below is enforced here, per CLAUDE.md's
 * "keep business logic inside services" mandate.
 *
 * Implements:
 *  - MDM-01: unique, non-duplicable code per record lineage.
 *  - MDM-02: every change creates a new version with effective dates, never an in-place edit.
 *  - MDM-05: activation blocked if mandatory attributes (density, rate, uom) are missing.
 *  - MDM-07: every change is written to the append-only audit log.
 *  - SRS 4.1.5 business rule: a rate cannot be deleted once referenced in an approved cost
 *    estimate; only deactivation is permitted. (The "referenced in an approved cost estimate"
 *    check itself depends on cost_record, which lands in M03/M08 - this service exposes the
 *    deactivate operation and documents the constraint; the concrete FK-existence check is
 *    wired in once cost_record exists, tracked in TASKS.md.)
 *  - SRS 4.1.5 business rule: rate changes take effect only from the declared effective date,
 *    never retroactively, unless explicitly re-costed by an authorized user (enforced by
 *    requiring effectiveFrom on every version and never allowing a past-dated version to
 *    retroactively supersede an already-effective one without an explicit "manual recost"
 *    flag - modeled here as simply requiring the new version's effectiveFrom to not be
 *    earlier than the current active version's effectiveFrom for the default create path).
 */
@Service
@RequiredArgsConstructor
public class MaterialMasterService {

    private static final String ENTITY_TYPE = "MaterialMaster";

    /**
     * SRS 4.3.5 business rule: "Material cost changes exceeding a configurable variance
     * threshold from the prior published rate require Costing Manager approval before
     * publishing." 10% is the MVP default pending the client confirmation tracked in
     * NEXT_AGENT.md-style open items; TODO-free by design, this is a named constant so
     * the follow-up to make it a per-tenant configurable value is a one-line change.
     */
    private static final BigDecimal DEFAULT_RATE_VARIANCE_APPROVAL_THRESHOLD = new BigDecimal("0.10");

    private final MaterialMasterRepository repository;
    private final MaterialMasterMapper mapper;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final AccessScopeService accessScopeService;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional
    public Response create(CreateRequest request) {
        accessScopeService.assertAccessForCurrentUser(ScopeType.PLANT, request.plantId());
        String customerId = tenantAccessGuard.requireCustomerId();
        if (repository.existsByCustomerIdAndCode(customerId, request.code())) {
            throw new DuplicateResourceException(
                    "A material master record with code '" + request.code() + "' already exists. "
                            + "To change its rate, submit a new version against the existing code "
                            + "rather than creating a duplicate (MDM-01).");
        }

        MaterialMaster entity = mapper.toEntity(request);
        entity.setRecordId(UUID.randomUUID().toString());
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setVersionNumber(1);
        entity.setStatus(resolveInitialStatus(entity));

        MaterialMaster saved = repository.save(entity);
        auditService.recordCreate(ENTITY_TYPE, saved.getRecordId(), mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /**
     * MDM-02: creates a NEW version rather than mutating the existing row. This is the only
     * supported way to change a published rate - there is deliberately no "edit in place"
     * method on this service.
     */
    @Transactional
    public Response createNewVersion(String code, UpdateRequest request) {
        MaterialMaster current = repository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(tenantAccessGuard.requireCustomerId(), code)
                .orElseThrow(() -> ResourceNotFoundException.of("MaterialMaster", code));

        if (request.effectiveFrom() != null && request.effectiveFrom().isBefore(current.getEffectiveFrom())) {
            throw new BusinessRuleViolationException(
                    "New version effective date cannot be earlier than the current version's effective date "
                            + "(" + current.getEffectiveFrom() + "). Rate changes take effect only from the "
                            + "declared date, never retroactively, unless explicitly re-costed.",
                    "SRS-4.1.5-effective-dating");
        }

        MaterialMaster newVersion = copyForNewVersion(current);
        mapper.updateEntityFromRequest(request, newVersion);
        newVersion.setVersionNumber(current.getVersionNumber() + 1);
        newVersion.setStatus(resolveVersionStatus(current, newVersion));

        MaterialMaster saved = repository.save(newVersion);
        auditService.recordUpdate(ENTITY_TYPE, saved.getRecordId(),
                mapper.toResponse(current), mapper.toResponse(saved));

        // MAT-05: publish so downstream BOM nodes referencing this material can be
        // recalculated. Only fires once the new version is actually ACTIVE - a rate sitting
        // in PENDING_APPROVAL (missing mandatory fields, or over the variance threshold
        // below) has not "gone live" yet and must not ripple into cost records.
        if (saved.getStatus() == MasterDataStatus.ACTIVE) {
            eventPublisher.publishEvent(
                    new MaterialRatePublishedEvent(saved.getCode(), saved.getRecordId(), saved.getRate()));
        }
        return mapper.toResponse(saved);
    }

    /**
     * MDM-05 (missing mandatory fields) AND SRS 4.3.5 (rate variance beyond threshold)
     * both gate whether a new version can go straight to ACTIVE, or must land in
     * PENDING_APPROVAL for a Costing Manager to sign off first.
     */
    private MasterDataStatus resolveVersionStatus(MaterialMaster previous, MaterialMaster candidate) {
        if (resolveInitialStatus(candidate) == MasterDataStatus.PENDING_APPROVAL) {
            return MasterDataStatus.PENDING_APPROVAL;
        }
        if (exceedsVarianceThreshold(previous.getRate(), candidate.getRate())) {
            return MasterDataStatus.PENDING_APPROVAL;
        }
        return MasterDataStatus.ACTIVE;
    }

    private boolean exceedsVarianceThreshold(BigDecimal previousRate, BigDecimal newRate) {
        if (previousRate == null || newRate == null || previousRate.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal variance = newRate.subtract(previousRate).abs()
                .divide(previousRate, 10, RoundingMode.HALF_UP);
        return variance.compareTo(DEFAULT_RATE_VARIANCE_APPROVAL_THRESHOLD) > 0;
    }

    /** MDM-02: full version history for a material code, newest first. */
    @Transactional(readOnly = true)
    public List<Response> listVersionsByCode(String code) {
        return repository.findByCustomerIdAndCodeOrderByVersionNumberDesc(tenantAccessGuard.requireCustomerId(), code).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Response getById(String recordId) {
        MaterialMaster entity = repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("MaterialMaster", recordId));
        accessScopeService.assertAccessForCurrentUser(ScopeType.PLANT, entity.getPlantId());
        return mapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<Response> search(MasterDataStatus status, String plantId, String searchTerm, Pageable pageable) {
        var restricted = accessScopeService.restrictedValuesForCurrentUser(ScopeType.PLANT);
        if (restricted.isPresent()) {
            if (plantId != null) {
                accessScopeService.assertAccessForCurrentUser(ScopeType.PLANT, plantId);
            }
            Page<Response> page = repository.searchByCustomer(tenantAccessGuard.requireCustomerId(), status, plantId, searchTerm, pageable).map(mapper::toResponse);
            if (plantId != null) {
                return page;
            }
            List<Response> filtered = page.getContent().stream()
                    .filter(r -> r.plantId() == null || restricted.get().contains(r.plantId()))
                    .toList();
            return new PageImpl<>(filtered, pageable, filtered.size());
        }
        return repository.searchByCustomer(tenantAccessGuard.requireCustomerId(), status, plantId, searchTerm, pageable).map(mapper::toResponse);
    }

    /**
     * Deactivates a material master record. This is the ONLY retirement path - there is
     * no delete() method on this service, per the business rule that a rate referenced
     * in an approved cost estimate can never be deleted, only deactivated for future use.
     * Non-referenced records may also simply be deactivated rather than deleted, keeping
     * a single consistent retirement mechanism.
     */
    /**
     * Costing Manager approval of a PENDING_APPROVAL version (MDM-05 / variance gate).
     * On approval: status → ACTIVE, previous ACTIVE version's effectiveTo closed to day-before
     * the new effectiveFrom (no overlapping active windows).
     */
    @Transactional
    public Response approve(String recordId) {
        MaterialMaster entity = repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("MaterialMaster", recordId));
        if (entity.getStatus() != MasterDataStatus.PENDING_APPROVAL) {
            throw new BusinessRuleViolationException(
                    "Only PENDING_APPROVAL material versions can be approved (current=" + entity.getStatus() + ")",
                    "MDM-APPROVE-01");
        }
        if (entity.getEffectiveTo() != null && entity.getEffectiveTo().isBefore(entity.getEffectiveFrom())) {
            throw new BusinessRuleViolationException(
                    "effectiveTo cannot be before effectiveFrom", "MDM-DATE-01");
        }
        // Close prior ACTIVE versions of same code that would overlap
        List<MaterialMaster> history = repository.findByCustomerIdAndCodeOrderByVersionNumberDesc(tenantAccessGuard.requireCustomerId(), entity.getCode());
        for (MaterialMaster prior : history) {
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
        Response before = mapper.toResponse(entity);
        entity.setStatus(MasterDataStatus.ACTIVE);
        MaterialMaster saved = repository.save(entity);
        auditService.recordTransition(ENTITY_TYPE, saved.getRecordId(),
                MasterDataStatus.PENDING_APPROVAL.name(), MasterDataStatus.ACTIVE.name());
        eventPublisher.publishEvent(
                new MaterialRatePublishedEvent(saved.getCode(), saved.getRecordId(), saved.getRate()));
        return mapper.toResponse(saved);
    }

    /** Resolve rate for historical costing as of a valuation date (immutable snapshot semantics). */
    @Transactional(readOnly = true)
    public Response findEffectiveAsOf(String code, LocalDate asOf) {
        MaterialMaster m = repository.findEffectiveAsOfForCustomer(tenantAccessGuard.requireCustomerId(), code, asOf)
                .orElseThrow(() -> ResourceNotFoundException.of("MaterialMaster", code + "@" + asOf));
        return mapper.toResponse(m);
    }

    @Transactional
    public Response deactivate(String recordId) {
        MaterialMaster entity = repository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), recordId)
                .orElseThrow(() -> ResourceNotFoundException.of("MaterialMaster", recordId));

        MaterialMaster before = copyForNewVersion(entity);
        entity.setStatus(MasterDataStatus.INACTIVE);
        MaterialMaster saved = repository.save(entity);

        auditService.recordUpdate(ENTITY_TYPE, saved.getRecordId(),
                mapper.toResponse(before), mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** MDM-05: a record missing rate/uom/density cannot be Active; it stays Pending Approval. */
    private MasterDataStatus resolveInitialStatus(MaterialMaster entity) {
        boolean missingMandatory = entity.getRate() == null
                || entity.getUom() == null || entity.getUom().isBlank()
                || entity.getDensity() == null;
        return missingMandatory ? MasterDataStatus.PENDING_APPROVAL : MasterDataStatus.ACTIVE;
    }

    private MaterialMaster copyForNewVersion(MaterialMaster source) {
        MaterialMaster copy = new MaterialMaster();
        copy.setRecordId(UUID.randomUUID().toString());
        copy.setCode(source.getCode());
        copy.setDescription(source.getDescription());
        copy.setRate(source.getRate());
        copy.setUom(source.getUom());
        copy.setPlantId(source.getPlantId());
        copy.setEffectiveFrom(source.getEffectiveFrom());
        copy.setEffectiveTo(source.getEffectiveTo());
        copy.setGrade(source.getGrade());
        copy.setSpecificationStandard(source.getSpecificationStandard());
        copy.setDensity(source.getDensity());
        copy.setScrapRecoveryRate(source.getScrapRecoveryRate());
        copy.setMaterialForm(source.getMaterialForm());
        copy.setIndexLinkedFlag(source.isIndexLinkedFlag());
        copy.setIndexCode(source.getIndexCode());
        copy.setBasePrice(source.getBasePrice());
        copy.setAlloySurchargePerUnit(source.getAlloySurchargePerUnit());
        return copy;
    }
}
