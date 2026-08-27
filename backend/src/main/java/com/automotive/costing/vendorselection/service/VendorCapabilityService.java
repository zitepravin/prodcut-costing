package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CapabilityResponse;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreateCapabilityRequest;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.EligibilityResponse;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.EvaluateEligibilityRequest;
import com.automotive.costing.vendorselection.engine.EligibilityGateEngine;
import com.automotive.costing.vendorselection.engine.EligibilityGateEngine.EvaluationInput;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierPolicyEvidence;
import com.automotive.costing.vendorselection.repository.SupplierPolicyEvidenceRepository;
import com.automotive.costing.vendorselection.repository.VendorQuestionnaireEvaluationRepository;
import com.automotive.costing.vendorselection.repository.SupplierRiskRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.automotive.costing.vendorselection.entity.RiskSeverity;
import com.automotive.costing.vendorselection.entity.RiskStatus;
import com.automotive.costing.vendorselection.entity.SupplierRisk;
import com.automotive.costing.vendorselection.entity.VendorQuestionnaireEvaluation;
import com.automotive.costing.vendorselection.entity.CapabilityStatus;
import com.automotive.costing.vendorselection.entity.CapabilityType;
import com.automotive.costing.vendorselection.entity.SupplierCapability;
import com.automotive.costing.vendorselection.entity.SupplierEligibilityResult;
import com.automotive.costing.vendorselection.repository.SupplierCapabilityRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.vendorselection.repository.SupplierEligibilityResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorCapabilityService {

    private final SupplierCapabilityRepository capabilityRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final SupplierPolicyEvidenceRepository policyRepository;
    private final VendorQuestionnaireEvaluationRepository questionnaireEvaluationRepository;
    private final SupplierRiskRepository riskRepository;
    private final SupplierQualificationCurrentRepository qualificationCurrentRepository;
    private final SupplierEligibilityResultRepository eligibilityResultRepository;
    private final SupplierMasterService supplierMasterService;
    private final EligibilityGateEngine eligibilityGateEngine;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CapabilityResponse create(CreateCapabilityRequest request) {
        supplierMasterService.require(request.supplierId());
        SupplierCapability c = new SupplierCapability();
        c.setCapabilityId(UUID.randomUUID().toString());
        c.setCustomerId(tenantAccessGuard.requireCustomerId());
        c.setSupplierId(request.supplierId());
        c.setCapabilityType(request.capabilityType());
        c.setCapabilityCode(request.capabilityCode().trim());
        c.setCapabilityName(request.capabilityName().trim());
        c.setProcessCode(request.processCode());
        c.setCommodityCode(request.commodityCode());
        c.setPlantCode(request.plantCode());
        c.setMaxAnnualCapacity(request.maxAnnualCapacity());
        c.setCapacityUom(request.capacityUom());
        c.setCertified(request.certified());
        c.setValidFrom(request.validFrom());
        c.setValidTo(request.validTo());
        c.setStatus(CapabilityStatus.ACTIVE);
        c.setNotes(request.notes());
        capabilityRepository.save(c);
        auditService.recordCreate("SupplierCapability", c.getCapabilityId(), c.getCapabilityCode());
        return toResponse(c);
    }

    /**
     * ACTIVE capabilities whose valid_to is on or before the given date (inclusive).
     * Used for certification/capability expiry operational views — does not invent expiry rules.
     */
    @Transactional(readOnly = true)
    public List<CapabilityResponse> listExpiring(java.time.LocalDate asOfDate) {
        if (asOfDate == null) {
            asOfDate = java.time.LocalDate.now();
        }
        String customerId = tenantAccessGuard.requireCustomerId();
        return capabilityRepository.findByStatusAndValidToLessThanEqualOrderByValidToAsc(
                        CapabilityStatus.ACTIVE, asOfDate)
                .stream()
                .filter(c -> customerId.equals(c.getCustomerId()))
                .map(c -> new CapabilityResponse(
                        c.getCapabilityId(), c.getSupplierId(), c.getCapabilityType(), c.getCapabilityCode(),
                        c.getCapabilityName(), c.getProcessCode(), c.getCommodityCode(), c.getPlantCode(),
                        c.getMaxAnnualCapacity(), c.getCapacityUom(), c.isCertified(),
                        c.getValidFrom(), c.getValidTo(), c.getStatus(), c.getNotes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CapabilityResponse> listBySupplier(String supplierId) {
        return listBySupplier(supplierId, null, true);
    }

    @Transactional(readOnly = true)
    public List<CapabilityResponse> listBySupplier(String supplierId, CapabilityType type, boolean activeOnly) {
        supplierMasterService.require(supplierId);
        java.util.List<SupplierCapability> rows;
        if (activeOnly && type != null) {
            rows = capabilityRepository.findBySupplierIdAndCapabilityTypeAndStatus(supplierId, type, CapabilityStatus.ACTIVE);
        } else if (activeOnly) {
            rows = capabilityRepository.findBySupplierIdAndStatus(supplierId, CapabilityStatus.ACTIVE);
        } else if (type != null) {
            rows = capabilityRepository.findBySupplierIdAndCapabilityTypeOrderByCapabilityCodeAsc(supplierId, type);
        } else {
            rows = capabilityRepository.findBySupplierIdOrderByCapabilityTypeAscCapabilityCodeAsc(supplierId);
        }
        return rows.stream().map(c -> new CapabilityResponse(
                c.getCapabilityId(), c.getSupplierId(), c.getCapabilityType(), c.getCapabilityCode(),
                c.getCapabilityName(), c.getProcessCode(), c.getCommodityCode(), c.getPlantCode(),
                c.getMaxAnnualCapacity(), c.getCapacityUom(), c.isCertified(),
                c.getValidFrom(), c.getValidTo(), c.getStatus(), c.getNotes())).toList();
    }

    @Transactional
    public EligibilityResponse evaluate(EvaluateEligibilityRequest request) {
        SupplierMaster supplier = supplierMasterService.require(request.supplierId());
        List<SupplierCapability> caps =
                capabilityRepository.findBySupplierIdAndStatus(request.supplierId(), CapabilityStatus.ACTIVE);

        LocalDate asOf = LocalDate.now();
        List<String> verifiedPolicies = policyRepository
                .findBySupplierIdOrderByCreatedAtDesc(request.supplierId())
                .stream()
                .filter(p -> p.getEvidenceStatus() == EvidenceStatus.VERIFIED)
                .filter(p -> p.getExpiryDate() == null || !asOf.isAfter(p.getExpiryDate()))
                .map(p -> p.getPolicyType().name())
                .distinct()
                .toList();

        String questionnaireEvaluationId = null;
        Boolean questionnaireEligible = null;
        if (request.requireQuestionnaire()) {
            var latestQ = questionnaireEvaluationRepository
                    .findFirstBySupplierIdOrderByEvaluatedAtDesc(request.supplierId());
            if (latestQ.isPresent()) {
                VendorQuestionnaireEvaluation qe = latestQ.get();
                questionnaireEvaluationId = qe.getEvaluationId();
                questionnaireEligible = qe.isOverallEligible();
            }
        }

        // Open risks (OPEN + MITIGATING count as open for gating)
        var openStatuses = java.util.EnumSet.of(RiskStatus.OPEN, RiskStatus.MITIGATING);
        List<SupplierRisk> openRisks = riskRepository
                .findBySupplierIdOrderByIdentifiedAtDesc(request.supplierId())
                .stream()
                .filter(r -> openStatuses.contains(r.getStatus()))
                .toList();
        int openCritical = (int) openRisks.stream().filter(r -> r.getSeverity() == RiskSeverity.CRITICAL).count();
        int openHigh = (int) openRisks.stream().filter(r -> r.getSeverity() == RiskSeverity.HIGH).count();
        int openTotal = openRisks.size();

        String lifecycleStatus = qualificationCurrentRepository.findById(request.supplierId())
                .map(SupplierQualificationCurrent::getLifecycleStatus)
                .map(Enum::name)
                .orElse(null);
        boolean lifecycleBlocked = lifecycleStatus != null
                && ("BLOCKED".equalsIgnoreCase(lifecycleStatus) || "SUSPENDED".equalsIgnoreCase(lifecycleStatus));

        var output = eligibilityGateEngine.evaluate(new EvaluationInput(
                supplier,
                caps,
                request.requiredCapabilityCodes(),
                request.requiredMinCapacity(),
                request.requireCertification(),
                verifiedPolicies,
                request.requiredPolicyTypes(),
                request.requireQuestionnaire(),
                questionnaireEligible,
                request.requireNoCriticalOpenRisks(),
                request.requireNoHighOpenRisks(),
                openCritical,
                openHigh,
                lifecycleStatus,
                asOf));

        SupplierEligibilityResult result = new SupplierEligibilityResult();
        result.setResultId(UUID.randomUUID().toString());
        result.setSupplierId(request.supplierId());
        result.setBomNodeId(request.bomNodeId());
        result.setProgramId(request.programId());
        result.setOverallEligible(output.overallEligible());
        result.setFailedGates(String.join(",", output.failedGates()));
        try {
            result.setEvaluationJson(objectMapper.writeValueAsString(output.gates()));
        } catch (JsonProcessingException e) {
            result.setEvaluationJson("[]");
        }
        result.setEvaluatedAt(Instant.now());
        result.setRequireQuestionnaire(request.requireQuestionnaire());
        result.setQuestionnaireEvaluationId(questionnaireEvaluationId);
        result.setQuestionnaireEligible(questionnaireEligible);
        result.setRequireNoCriticalOpenRisks(request.requireNoCriticalOpenRisks());
        result.setRequireNoHighOpenRisks(request.requireNoHighOpenRisks());
        result.setOpenCriticalRiskCount(openCritical);
        result.setOpenHighRiskCount(openHigh);
        result.setOpenRiskCount(openTotal);
        result.setLifecycleStatus(lifecycleStatus);
        result.setLifecycleBlocked(lifecycleBlocked);
        eligibilityResultRepository.save(result);
        auditService.recordCreate("SupplierEligibilityResult", result.getResultId(),
                request.supplierId() + " eligible=" + output.overallEligible());

        return new EligibilityResponse(
                result.getResultId(),
                result.getSupplierId(),
                result.getBomNodeId(),
                result.getProgramId(),
                result.isOverallEligible(),
                output.failedGates(),
                output.gates(),
                result.getEvaluatedAt(),
                questionnaireEvaluationId,
                questionnaireEligible,
                openCritical,
                openHigh,
                openTotal,
                lifecycleStatus,
                lifecycleBlocked);
    }

    private CapabilityResponse toResponse(SupplierCapability c) {
        return new CapabilityResponse(
                c.getCapabilityId(),
                c.getSupplierId(),
                c.getCapabilityType(),
                c.getCapabilityCode(),
                c.getCapabilityName(),
                c.getProcessCode(),
                c.getCommodityCode(),
                c.getPlantCode(),
                c.getMaxAnnualCapacity(),
                c.getCapacityUom(),
                c.isCertified(),
                c.getValidFrom(),
                c.getValidTo(),
                c.getStatus(),
                c.getNotes());
    }
}
