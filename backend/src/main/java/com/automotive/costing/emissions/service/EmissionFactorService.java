package com.automotive.costing.emissions.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.emissions.dto.EmissionsDtos.CreateFactorRequest;
import com.automotive.costing.emissions.dto.EmissionsDtos.FactorResponse;
import com.automotive.costing.emissions.entity.EmissionFactor;
import com.automotive.costing.emissions.repository.EmissionFactorRepository;
import com.automotive.costing.masterdata.entity.MaterialMaster;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * P3-11a emission factor master. Human-maintained; missing factors never invent values.
 */
@Service
@RequiredArgsConstructor
public class EmissionFactorService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RETIRED = "RETIRED";
    public static final String UNIT_KG_PER_KG = "KG_CO2E_PER_KG";
    public static final String UNIT_KG_PER_HOUR = "KG_CO2E_PER_HOUR";
    public static final String CAT_MATERIAL = "MATERIAL";
    public static final String CAT_PROCESS = "PROCESS";

    private final EmissionFactorRepository factorRepository;
    private final MaterialMasterRepository materialMasterRepository;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional(readOnly = true)
    public List<FactorResponse> list() {
        return factorRepository.findAllByOrderByFactorCodeAscVersionNoAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FactorResponse create(CreateFactorRequest request) {
        if (request.co2eValue() == null || request.co2eValue().signum() < 0) {
            throw new IllegalArgumentException("co2eValue must be >= 0");
        }
        String code = request.factorCode().trim().toUpperCase(Locale.ROOT);
        if (factorRepository.existsByFactorCodeIgnoreCaseAndVersionNo(code, 1)) {
            throw new BusinessRuleViolationException("Factor code already exists at version 1", "EM-01");
        }
        String category = request.category() == null || request.category().isBlank()
                ? CAT_MATERIAL : request.category().trim().toUpperCase(Locale.ROOT);
        if (!CAT_MATERIAL.equals(category) && !CAT_PROCESS.equals(category)) {
            throw new BusinessRuleViolationException("category must be MATERIAL or PROCESS", "EM-07");
        }
        String unit = request.unit() == null || request.unit().isBlank()
                ? (CAT_PROCESS.equals(category) ? UNIT_KG_PER_HOUR : UNIT_KG_PER_KG)
                : request.unit().trim().toUpperCase(Locale.ROOT);
        if (CAT_MATERIAL.equals(category) && !UNIT_KG_PER_KG.equals(unit)) {
            throw new BusinessRuleViolationException("MATERIAL factor unit must be KG_CO2E_PER_KG", "EM-08");
        }
        if (CAT_PROCESS.equals(category) && !UNIT_KG_PER_HOUR.equals(unit)) {
            throw new BusinessRuleViolationException("PROCESS factor unit must be KG_CO2E_PER_HOUR", "EM-09");
        }
        EmissionFactor f = new EmissionFactor();
        f.setFactorId(UUID.randomUUID().toString());
        f.setFactorCode(code);
        f.setFactorName(request.factorName().trim());
        f.setCategory(category);
        f.setCo2eValue(request.co2eValue());
        f.setUnit(unit);
        f.setSourceType(request.sourceType() == null || request.sourceType().isBlank() ? "MANUAL" : request.sourceType().trim());
        f.setSourceReference(request.sourceReference());
        f.setGeography(request.geography());
        f.setVersionNo(1);
        f.setStatus(STATUS_DRAFT);
        f.setEffectiveFrom(request.effectiveFrom());
        f.setEffectiveTo(request.effectiveTo());
        factorRepository.save(f);
        return toResponse(f);
    }

    @Transactional
    public FactorResponse publish(String factorId) {
        EmissionFactor f = factorRepository.findById(factorId)
                .orElseThrow(() -> new ResourceNotFoundException("Emission factor", factorId));
        if (STATUS_RETIRED.equals(f.getStatus())) {
            throw new BusinessRuleViolationException("Retired factor cannot be published", "EM-02");
        }
        if (f.getCo2eValue() == null || f.getCo2eValue().signum() < 0) {
            throw new BusinessRuleViolationException("Invalid co2eValue", "EM-03");
        }
        f.setStatus(STATUS_ACTIVE);
        factorRepository.save(f);
        return toResponse(f);
    }

    @Transactional
    public FactorResponse retire(String factorId) {
        EmissionFactor f = factorRepository.findById(factorId)
                .orElseThrow(() -> new ResourceNotFoundException("Emission factor", factorId));
        f.setStatus(STATUS_RETIRED);
        factorRepository.save(f);
        return toResponse(f);
    }

    @Transactional
    public void linkMaterialDefault(String materialRecordId, String factorId) {
        MaterialMaster m = materialMasterRepository.findByCustomerIdAndRecordId(tenantAccessGuard.requireCustomerId(), materialRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Material master", materialRecordId));
        EmissionFactor f = factorRepository.findById(factorId)
                .orElseThrow(() -> new ResourceNotFoundException("Emission factor", factorId));
        if (!STATUS_ACTIVE.equals(f.getStatus())) {
            throw new BusinessRuleViolationException("Only ACTIVE factors can be linked as material default", "EM-04");
        }
        if (!"MATERIAL".equals(f.getCategory())) {
            throw new BusinessRuleViolationException("Factor category must be MATERIAL", "EM-05");
        }
        m.setDefaultEmissionFactorId(f.getFactorId());
        materialMasterRepository.save(m);
    }

    @Transactional(readOnly = true)
    public EmissionFactor requireActive(String factorId) {
        EmissionFactor f = factorRepository.findById(factorId)
                .orElseThrow(() -> new ResourceNotFoundException("Emission factor", factorId));
        if (!STATUS_ACTIVE.equals(f.getStatus())) {
            throw new BusinessRuleViolationException("Emission factor is not ACTIVE", "EM-06");
        }
        return f;
    }

    private FactorResponse toResponse(EmissionFactor f) {
        return new FactorResponse(
                f.getFactorId(), f.getFactorCode(), f.getFactorName(), f.getCategory(),
                f.getCo2eValue(), f.getUnit(), f.getSourceType(), f.getSourceReference(),
                f.getGeography(), f.getVersionNo(), f.getStatus(),
                f.getEffectiveFrom(), f.getEffectiveTo());
    }
}
