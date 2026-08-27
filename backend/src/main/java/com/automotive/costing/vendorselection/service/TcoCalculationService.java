package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.service.SupplierLifecycleGuard;
import com.automotive.costing.vendorselection.engine.TcoCalculationEngine;
import com.automotive.costing.vendorselection.engine.TcoCalculationEngine.TcoInput;
import com.automotive.costing.vendorselection.entity.SupplierTcoCalculation;
import com.automotive.costing.vendorselection.repository.SupplierTcoCalculationRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TcoCalculationService {

    private final TcoCalculationEngine tcoCalculationEngine;
    private final SupplierTcoCalculationRepository tcoRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final SupplierMasterService supplierMasterService;
    private final AuditService auditService;
    private final SupplierLifecycleGuard lifecycleGuard;
    private final ObjectMapper objectMapper;

    public record CalculateTcoRequest(
            String supplierId,
            String bomNodeId,
            String programId,
            String currencyCode,
            BigDecimal annualVolume,
            BigDecimal purchasePrice,
            BigDecimal freight,
            BigDecimal logistics,
            BigDecimal packaging,
            BigDecimal inventoryCost,
            BigDecimal qualityRejectionCost,
            BigDecimal reworkCost,
            BigDecimal warrantyCost,
            BigDecimal serviceCost,
            BigDecimal toolingCost,
            BigDecimal otherCost
    ) {}

    public record TcoResponse(
            String tcoId,
            String supplierId,
            String bomNodeId,
            String programId,
            String currencyCode,
            BigDecimal annualVolume,
            BigDecimal tcoTotal,
            BigDecimal unitTco,
            boolean dataComplete,
            List<String> missingComponents,
            List<TcoCalculationEngine.ComponentLine> components,
            Instant calculatedAt
    ) {}

    @Transactional
    public TcoResponse calculate(CalculateTcoRequest request) {
        if (request.supplierId() == null || request.supplierId().isBlank()) {
            throw new BusinessRuleViolationException("supplierId is required", "VS-TCO-01");
        }
        if (request.currencyCode() == null || request.currencyCode().isBlank()) {
            throw new BusinessRuleViolationException("currencyCode is required", "VS-TCO-02");
        }
        supplierMasterService.require(request.supplierId());
        lifecycleGuard.assertNotBlockedOrSuspended(
                request.supplierId(), SupplierLifecycleGuard.OP_TCO_CALCULATE);

        var output = tcoCalculationEngine.calculate(new TcoInput(
                request.purchasePrice(),
                request.freight(),
                request.logistics(),
                request.packaging(),
                request.inventoryCost(),
                request.qualityRejectionCost(),
                request.reworkCost(),
                request.warrantyCost(),
                request.serviceCost(),
                request.toolingCost(),
                request.otherCost(),
                request.annualVolume()));

        SupplierTcoCalculation entity = new SupplierTcoCalculation();
        entity.setTcoId(UUID.randomUUID().toString());
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setSupplierId(request.supplierId());
        entity.setBomNodeId(request.bomNodeId());
        entity.setProgramId(request.programId());
        entity.setCurrencyCode(request.currencyCode().trim().toUpperCase());
        entity.setAnnualVolume(request.annualVolume());
        entity.setPurchasePrice(request.purchasePrice());
        entity.setFreight(request.freight());
        entity.setLogistics(request.logistics());
        entity.setPackaging(request.packaging());
        entity.setInventoryCost(request.inventoryCost());
        entity.setQualityRejectionCost(request.qualityRejectionCost());
        entity.setReworkCost(request.reworkCost());
        entity.setWarrantyCost(request.warrantyCost());
        entity.setServiceCost(request.serviceCost());
        entity.setToolingCost(request.toolingCost());
        entity.setOtherCost(request.otherCost());
        entity.setTcoTotal(output.tcoTotal());
        entity.setMissingComponents(String.join(",", output.missingComponents()));
        entity.setDataComplete(output.dataComplete());
        try {
            entity.setCalculationJson(objectMapper.writeValueAsString(output.components()));
        } catch (JsonProcessingException e) {
            entity.setCalculationJson("[]");
        }
        entity.setCalculatedAt(Instant.now());
        entity.setCalculatedBy(currentUserId());
        tcoRepository.save(entity);
        auditService.recordCreate("SupplierTcoCalculation", entity.getTcoId(),
                request.supplierId() + " total=" + output.tcoTotal());

        return new TcoResponse(
                entity.getTcoId(),
                entity.getSupplierId(),
                entity.getBomNodeId(),
                entity.getProgramId(),
                entity.getCurrencyCode(),
                entity.getAnnualVolume(),
                output.tcoTotal(),
                output.unitTco(),
                output.dataComplete(),
                output.missingComponents(),
                output.components(),
                entity.getCalculatedAt());
    }

    @Transactional(readOnly = true)
    public List<TcoResponse> listBySupplier(String supplierId) {
        return tcoRepository.findByCustomerIdAndSupplierIdOrderByCalculatedAtDesc(
                tenantAccessGuard.requireCustomerId(), supplierId).stream()
                .map(this::toResponse)
                .toList();
    }

    private TcoResponse toResponse(SupplierTcoCalculation e) {
        return new TcoResponse(
                e.getTcoId(), e.getSupplierId(), e.getBomNodeId(), e.getProgramId(),
                e.getCurrencyCode(), e.getAnnualVolume(), e.getTcoTotal(), null,
                e.isDataComplete(),
                e.getMissingComponents() == null || e.getMissingComponents().isBlank()
                        ? List.of() : List.of(e.getMissingComponents().split(",")),
                List.of(), e.getCalculatedAt());
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.automotive.costing.platform.user.entity.User u) {
            return u.getUserId();
        }
        return null;
    }
}
