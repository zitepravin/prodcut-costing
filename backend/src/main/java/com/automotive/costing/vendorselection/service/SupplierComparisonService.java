package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.SupplierComparisonEngine;
import com.automotive.costing.vendorselection.engine.SupplierComparisonEngine.SupplierFacts;
import com.automotive.costing.vendorselection.entity.CapabilityStatus;
import com.automotive.costing.vendorselection.entity.QualityMetricType;
import com.automotive.costing.vendorselection.entity.VendorComparison;
import com.automotive.costing.vendorselection.repository.SupplierCapabilityRepository;
import com.automotive.costing.vendorselection.repository.SupplierDeliveryMetricRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualityMetricRepository;
import com.automotive.costing.vendorselection.repository.SupplierTcoCalculationRepository;
import com.automotive.costing.vendorselection.repository.SupplierWarrantyTermRepository;
import com.automotive.costing.vendorselection.repository.VendorComparisonRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.automotive.costing.vendorselection.repository.VendorScorecardResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierComparisonService {

    private final SupplierMasterService supplierMasterService;
    private final SupplierCapabilityRepository capabilityRepository;
    private final SupplierQualityMetricRepository qualityRepository;
    private final SupplierDeliveryMetricRepository deliveryRepository;
    private final SupplierWarrantyTermRepository warrantyRepository;
    private final SupplierTcoCalculationRepository tcoRepository;
    private final VendorScorecardResultRepository scorecardResultRepository;
    private final VendorComparisonRepository comparisonRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final SupplierQualificationCurrentRepository qualificationCurrentRepository;
    private final SupplierComparisonEngine comparisonEngine;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public record CompareRequest(
            List<String> supplierIds,
            String bomNodeId,
            String programId,
            String currencyCode,
            String notes
    ) {}

    public record ComparisonResponse(
            String comparisonId,
            List<String> supplierIds,
            List<SupplierComparisonEngine.ComparisonRow> rows,
            String tradeOffSummary,
            String notes,
            List<String> lifecycleBlockedSupplierIds,
            String lifecycleSummary
    ) {}

    @Transactional
    public ComparisonResponse compareAndPersist(CompareRequest request) {
        if (request.supplierIds() == null || request.supplierIds().size() < 2) {
            throw new BusinessRuleViolationException(
                    "At least two supplierIds are required for comparison", "VS-CMP-01");
        }
        List<SupplierFacts> facts = new ArrayList<>();
        for (String supplierId : request.supplierIds()) {
            facts.add(loadFacts(supplierId));
        }
        var output = comparisonEngine.compare(facts);

        VendorComparison entity = new VendorComparison();
        entity.setComparisonId(UUID.randomUUID().toString());
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setBomNodeId(request.bomNodeId());
        entity.setProgramId(request.programId());
        entity.setCurrencyCode(request.currencyCode());
        entity.setSupplierIdsCsv(String.join(",", request.supplierIds()));
        entity.setNotes(request.notes());
        try {
            entity.setComparisonJson(objectMapper.writeValueAsString(output));
        } catch (JsonProcessingException e) {
            entity.setComparisonJson("{}");
        }
        comparisonRepository.save(entity);
        auditService.recordCreate("VendorComparison", entity.getComparisonId(),
                entity.getSupplierIdsCsv());

        return new ComparisonResponse(
                entity.getComparisonId(),
                output.supplierIds(),
                output.rows(),
                output.tradeOffSummary(),
                request.notes(),
                output.lifecycleBlockedSupplierIds(),
                entity.getLifecycleSummary());
    }


    public record ComparisonSummaryResponse(
            String comparisonId,
            String bomNodeId,
            String programId,
            String currencyCode,
            String supplierIdsCsv,
            String notes,
            String lifecycleSummary,
            java.time.Instant createdAt
    ) {}

    @Transactional(readOnly = true)
    public List<ComparisonSummaryResponse> listRecent() {
        return comparisonRepository.findTop50ByCustomerIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComparisonSummaryResponse> listByBomNode(String bomNodeId) {
        if (bomNodeId == null || bomNodeId.isBlank()) {
            throw new BusinessRuleViolationException("bomNodeId is required", "VS-CMP-10");
        }
        return comparisonRepository.findByCustomerIdAndBomNodeIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), bomNodeId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComparisonSummaryResponse> listByProgram(String programId) {
        if (programId == null || programId.isBlank()) {
            throw new BusinessRuleViolationException("programId is required", "VS-CMP-11");
        }
        return comparisonRepository.findByCustomerIdAndProgramIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), programId).stream()
                .map(this::toSummary)
                .toList();
    }

    private ComparisonSummaryResponse toSummary(VendorComparison c) {
        return new ComparisonSummaryResponse(
                c.getComparisonId(),
                c.getBomNodeId(),
                c.getProgramId(),
                c.getCurrencyCode(),
                c.getSupplierIdsCsv(),
                c.getNotes(),
                c.getLifecycleSummary(),
                c.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ComparisonResponse get(String comparisonId) {
        VendorComparison c = comparisonRepository.findById(comparisonId)
                .orElseThrow(() -> ResourceNotFoundException.of("VendorComparison", comparisonId));
        try {
            var output = objectMapper.readValue(c.getComparisonJson(),
                    SupplierComparisonEngine.ComparisonOutput.class);
            return new ComparisonResponse(c.getComparisonId(), output.supplierIds(),
                    output.rows(), output.tradeOffSummary(), c.getNotes(),
                    output.lifecycleBlockedSupplierIds(),
                    c.getLifecycleSummary());
        } catch (JsonProcessingException e) {
            throw new BusinessRuleViolationException("Stored comparison JSON is invalid", "VS-CMP-02");
        }
    }

    private SupplierFacts loadFacts(String supplierId) {
        SupplierMaster supplier = supplierMasterService.require(supplierId);
        int caps = capabilityRepository.findBySupplierIdAndStatus(supplierId, CapabilityStatus.ACTIVE).size();

        BigDecimal ppm = qualityRepository.findBySupplierIdOrderByPeriodEndDesc(supplierId).stream()
                .filter(m -> m.getMetricType() == QualityMetricType.PPM)
                .map(m -> m.getMetricValue())
                .findFirst().orElse(null);

        BigDecimal otd = deliveryRepository.findBySupplierIdOrderByPeriodEndDesc(supplierId).stream()
                .map(m -> m.getOtdPercent())
                .filter(v -> v != null)
                .findFirst().orElse(null);

        Integer warrantyMonths = warrantyRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream()
                .map(w -> w.getDurationMonths())
                .filter(v -> v != null)
                .findFirst().orElse(null);

        var tco = tcoRepository.findBySupplierIdOrderByCalculatedAtDesc(supplierId).stream().findFirst();
        BigDecimal tcoTotal = tco.map(t -> t.getTcoTotal()).orElse(null);
        boolean tcoComplete = tco.map(t -> t.isDataComplete()).orElse(false);
        List<String> tcoMissing = tco
                .map(t -> t.getMissingComponents() == null || t.getMissingComponents().isBlank()
                        ? List.<String>of()
                        : List.of(t.getMissingComponents().split(",")))
                .orElse(List.of("NO_TCO_CALCULATION"));

        var score = scorecardResultRepository.findBySupplierIdOrderByScoredAtDesc(supplierId).stream()
                .findFirst();
        BigDecimal overall = score.map(s -> s.getOverallScore()).orElse(null);

        List<String> gaps = new ArrayList<>();
        if (overall == null) gaps.add("No scorecard result");
        if (!tcoComplete) gaps.add("TCO incomplete or missing");
        if (ppm == null) gaps.add("No PPM metric");
        if (otd == null) gaps.add("No OTD metric");
        if (warrantyMonths == null) gaps.add("No warranty term");
        if (caps == 0) gaps.add("No active capabilities");

        // Eligibility is not re-run here without required capability context —
        // surface last known incomplete data rather than inventing eligibility.
        boolean eligible = caps > 0;
        List<String> failedGates = eligible ? List.of() : List.of("PART_PROCESS_CAPABILITY");

        String lifecycle = qualificationCurrentRepository.findById(supplierId)
                .map(SupplierQualificationCurrent::getLifecycleStatus)
                .map(Enum::name)
                .orElse(null);

        return new SupplierFacts(
                supplierId,
                supplier.getSupplierName() != null ? supplier.getSupplierName() : supplierId,
                eligible,
                failedGates,
                overall,
                tcoTotal,
                tcoComplete,
                tcoMissing,
                ppm,
                otd,
                warrantyMonths,
                caps,
                gaps,
                lifecycle);
    }
}
