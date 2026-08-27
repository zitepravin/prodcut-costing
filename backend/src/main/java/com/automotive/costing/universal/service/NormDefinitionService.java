package com.automotive.costing.universal.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.universal.dto.UniversalCostingDtos.NormRequest;
import com.automotive.costing.universal.dto.UniversalCostingDtos.NormResponse;
import com.automotive.costing.universal.entity.NormDefinition;
import com.automotive.costing.universal.repository.NormDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NormDefinitionService {

    private final NormDefinitionRepository repository;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional(readOnly = true)
    public List<NormResponse> list() {
        String customerId = tenantAccessGuard.requireCustomerId();
        return repository.findByCustomerIdOrderByNormCodeAscVersionNoDesc(customerId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public NormResponse create(NormRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        if (request.normCode() == null || request.normCode().isBlank()) {
            throw new BusinessRuleViolationException("normCode is required", "NORM-03");
        }
        validate(request);
        Instant now = Instant.now();
        NormDefinition n = new NormDefinition();
        n.setNormId(UUID.randomUUID().toString());
        n.setCustomerId(customerId);
        n.setNormCode(request.normCode().trim().toUpperCase(Locale.ROOT));
        n.setDescription(request.description());
        n.setUnit(request.unit());
        n.setParameterKey(request.parameterKey());
        n.setFormulaCode(request.formulaCode());
        n.setNormValue(request.normValue());
        n.setObjectScope(request.objectScope());
        n.setObjectReferenceId(request.objectReferenceId());
        n.setEffectiveFrom(request.effectiveFrom());
        n.setEffectiveTo(request.effectiveTo());
        n.setVersionNo(1);
        n.setActive(true);
        n.setApprovalStatus("DRAFT");
        n.setSource(request.source());
        n.setCreatedAt(now);
        n.setUpdatedAt(now);
        return toResponse(repository.save(n));
    }

    /**
     * Create a new version of an existing norm. Approved versions are immutable —
     * changes always produce a new version row. Optionally closes the prior version
     * effectiveTo = day before new effectiveFrom.
     */
    @Transactional
    public NormResponse createNewVersion(String priorNormId, NormRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        validate(request);

        NormDefinition prior = repository.findByCustomerIdAndNormId(customerId, priorNormId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Prior norm not found: " + priorNormId, "NORM-10"));

        String code = prior.getNormCode();
        if (request.normCode() != null && !request.normCode().isBlank()
                && !code.equalsIgnoreCase(request.normCode().trim())) {
            throw new BusinessRuleViolationException(
                    "Cannot change normCode on versioning; create a new norm instead", "NORM-11");
        }

        int nextVer = repository.findMaxVersionNo(customerId, code) + 1;
        Instant now = Instant.now();

        // Close prior open-ended version if it overlaps
        if (prior.getEffectiveTo() == null
                && request.effectiveFrom() != null
                && !request.effectiveFrom().isBefore(prior.getEffectiveFrom())) {
            LocalDate closeDate = request.effectiveFrom().minusDays(1);
            if (!closeDate.isBefore(prior.getEffectiveFrom())) {
                prior.setEffectiveTo(closeDate);
                prior.setUpdatedAt(now);
                repository.save(prior);
            }
        }

        NormDefinition n = new NormDefinition();
        n.setNormId(UUID.randomUUID().toString());
        n.setCustomerId(customerId);
        n.setNormCode(code);
        n.setDescription(request.description() != null ? request.description() : prior.getDescription());
        n.setUnit(request.unit() != null ? request.unit() : prior.getUnit());
        n.setParameterKey(request.parameterKey() != null ? request.parameterKey() : prior.getParameterKey());
        n.setFormulaCode(request.formulaCode() != null ? request.formulaCode() : prior.getFormulaCode());
        n.setNormValue(request.normValue());
        n.setObjectScope(request.objectScope() != null ? request.objectScope() : prior.getObjectScope());
        n.setObjectReferenceId(request.objectReferenceId() != null ? request.objectReferenceId() : prior.getObjectReferenceId());
        n.setEffectiveFrom(request.effectiveFrom());
        n.setEffectiveTo(request.effectiveTo());
        n.setVersionNo(nextVer);
        n.setActive(true);
        n.setApprovalStatus("DRAFT");
        n.setSource(request.source() != null ? request.source() : prior.getSource());
        n.setCreatedAt(now);
        n.setUpdatedAt(now);
        return toResponse(repository.save(n));
    }

    @Transactional
    public NormResponse approve(String normId, String actorUserId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        NormDefinition n = repository.findByCustomerIdAndNormId(customerId, normId)
                .orElseThrow(() -> new BusinessRuleViolationException("Norm not found: " + normId, "NORM-01"));
        if ("APPROVED".equals(n.getApprovalStatus())) {
            throw new BusinessRuleViolationException("Norm already approved and immutable", "NORM-02");
        }
        n.setApprovalStatus("APPROVED");
        n.setApprovedBy(actorUserId);
        n.setApprovedAt(Instant.now());
        n.setUpdatedAt(Instant.now());
        return toResponse(repository.save(n));
    }

    @Transactional(readOnly = true)
    public Optional<NormDefinition> resolveEffective(String normCode, LocalDate asOf) {
        String customerId = tenantAccessGuard.requireCustomerId();
        if (normCode == null || normCode.isBlank() || asOf == null) {
            return Optional.empty();
        }
        List<NormDefinition> list = repository.findEffective(
                customerId, normCode.trim().toUpperCase(Locale.ROOT), asOf);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private void validate(NormRequest request) {
        if (request.normCode() == null || request.normCode().isBlank()) {
            // allowed blank only when versioning inherits code — but create requires it
        }
        if (request.normValue() == null) {
            throw new BusinessRuleViolationException("normValue is required", "NORM-04");
        }
        if (request.effectiveFrom() == null) {
            throw new BusinessRuleViolationException("effectiveFrom is required", "NORM-05");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new BusinessRuleViolationException("effectiveTo must be on or after effectiveFrom", "NORM-06");
        }
    }

    private NormResponse toResponse(NormDefinition n) {
        return new NormResponse(
                n.getNormId(), n.getCustomerId(), n.getNormCode(), n.getDescription(),
                n.getUnit(), n.getParameterKey(), n.getFormulaCode(), n.getNormValue(),
                n.getObjectScope(), n.getObjectReferenceId(), n.getEffectiveFrom(),
                n.getEffectiveTo(), n.getVersionNo(), n.isActive(), n.getApprovalStatus(),
                n.getSource(), n.getCreatedAt());
    }
}
