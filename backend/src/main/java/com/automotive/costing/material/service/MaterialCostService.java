package com.automotive.costing.material.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.bom.service.BomNodeService;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.material.dto.MaterialCostDtos.*;
import com.automotive.costing.material.engine.MaterialCostEngine;
import com.automotive.costing.material.engine.MaterialCostEngine.MaterialCostInput;
import com.automotive.costing.material.engine.MaterialCostEngine.MaterialCostResult;
import com.automotive.costing.material.entity.MaterialCostCalculation;
import com.automotive.costing.material.entity.PricingBasis;
import com.automotive.costing.material.repository.MaterialCostCalculationRepository;
import com.automotive.costing.masterdata.entity.MaterialMaster;
import com.automotive.costing.masterdata.entity.ProcessMaster;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.masterdata.repository.ProcessMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * M03 Raw Material Cost Management (SRS Section 4.3; TDD Section 5.03/6). Orchestrates
 * {@link MaterialCostEngine} (the pure formula core) against real master data - resolving
 * the material's rate (fixed vs. index-linked, MAT-02), the applicable scrap% (process
 * master default vs. an explicitly justified override, SRS 4.3.5), and persisting the
 * result - then, per MAT-05, immediately rolls up the owning BOM node's program so the
 * calculation is reflected end-to-end rather than sitting stale until some later trigger.
 *
 * Carries the highest financial-calculation risk in the system per Test Plan Section 12
 * risk #1 ("a silent calculation error directly misleads multi-million-dollar sourcing
 * decisions") - every formula decision here defers to MaterialCostEngine, which is
 * unit-tested against hand-calculated control values; this class's own job is correctly
 * wiring inputs, not doing arithmetic itself.
 */
@Service
@RequiredArgsConstructor
public class MaterialCostService {

    private static final String ENTITY_TYPE = "MaterialCostCalculation";

    private final MaterialCostCalculationRepository calculationRepository;
    private final MaterialMasterRepository materialMasterRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final ProcessMasterRepository processMasterRepository;
    private final BomNodeRepository bomNodeRepository;
    private final CommodityIndexService commodityIndexService;
    private final AuditService auditService;
    private final BomNodeService bomNodeService;

    /**
     * MAT-01/02/03/06: calculates and persists the material cost for a BOM node, then
     * triggers a MAT-05-style roll-up of the owning program so the new figure is
     * immediately visible in the BOM tree rather than only in this calculation's own record.
     */
    @Transactional
    public Response calculate(CalculateRequest request) {
        BomNode bomNode = bomNodeRepository.findByCustomerIdAndBomNodeId(tenantAccessGuard.requireCustomerId(), request.bomNodeId())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", request.bomNodeId()));

        // SRS 4.3.5: "Net weight is sourced from the CAD/PLM part attribute wherever
        // available; manual entry requires supervisor approval."
        requireManualEntryApprovalIfApplicable(request);

        // Validate scrap override justification before any master-data lookups so pure
        // input-rule violations surface as BusinessRuleViolationException, not as
        // ResourceNotFoundException for an unrelated missing material.
        BigDecimal effectiveScrapPct = resolveScrapPct(request);

        MaterialMaster material = materialMasterRepository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(
                        tenantAccessGuard.requireCustomerId(), request.materialCode())
                .orElseThrow(() -> ResourceNotFoundException.of("MaterialMaster", request.materialCode()));
        LocalDate effectiveDate = request.effectiveDateOverride() != null
                ? request.effectiveDateOverride() : LocalDate.now();

        PricingBasis pricingBasis;
        BigDecimal unitRate;
        if (material.isIndexLinkedFlag()) {
            pricingBasis = PricingBasis.INDEX_LINKED;
            var indexValue = commodityIndexService.getEffectiveValue(material.getIndexCode(), effectiveDate);
            unitRate = MaterialCostEngine.computeIndexLinkedRate(
                    material.getBasePrice(), indexValue.getIndexValue(), material.getAlloySurchargePerUnit());
        } else {
            pricingBasis = PricingBasis.FIXED;
            unitRate = material.getRate();
        }

        MaterialCostResult result = MaterialCostEngine.calculate(new MaterialCostInput(
                request.netWeight(), effectiveScrapPct, unitRate, pricingBasis,
                material.getScrapRecoveryRate(), material.getMaterialForm()));

        MaterialCostCalculation entity = new MaterialCostCalculation();
        entity.setCalculationId(UUID.randomUUID().toString());
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setBomNodeId(request.bomNodeId());
        entity.setMaterialCode(request.materialCode());
        entity.setNetWeight(request.netWeight());
        entity.setScrapPct(effectiveScrapPct == null ? BigDecimal.ZERO : effectiveScrapPct);
        entity.setGrossWeight(result.grossWeight());
        entity.setUnitRate(unitRate);
        entity.setPricingBasis(pricingBasis);
        entity.setScrapRecoveryCredit(result.scrapRecoveryCredit());
        entity.setMaterialCost(result.materialCost());
        entity.setCalculationBasis(result.calculationBasis());
        entity.setCalculatedAt(Instant.now());

        MaterialCostCalculation saved = calculationRepository.save(entity);
        auditService.recordCreate(ENTITY_TYPE, saved.getCalculationId(), toResponse(saved));

        // Link the BOM node to this material if not already linked, then roll up the
        // whole program - MAT-05's "recalculate all dependent BOM and part costs".
        if (bomNode.getMaterialCode() == null || !bomNode.getMaterialCode().equals(request.materialCode())) {
            bomNode.setMaterialCode(request.materialCode());
            bomNodeRepository.save(bomNode);
        }
        bomNodeService.triggerRollupForProgram(bomNode.getProgramId());

        return toResponse(saved);
    }

    /**
     * MAT-04: compare material cost across two or more alternate grades/suppliers
     * (SRS requires "at least three" be supportable in a single view; this accepts any
     * list of two or more so smaller comparisons are not artificially blocked).
     */
    @Transactional(readOnly = true)
    public CompareResponse compareAlternates(CompareRequest request) {
        if (request.materialCodes() == null || request.materialCodes().size() < 2) {
            throw new BusinessRuleViolationException(
                    "At least two material codes are required to run a comparison.", "MAT-04");
        }

        List<CompareLine> lines = request.materialCodes().stream()
                .map(code -> compareOne(code, request.netWeight(), request.scrapPct()))
                .sorted(Comparator.comparing(CompareLine::materialCost))
                .toList();

        String lowest = lines.isEmpty() ? null : lines.get(0).materialCode();
        return new CompareResponse(request.netWeight(), request.scrapPct(), lines, lowest);
    }

    private CompareLine compareOne(String materialCode, BigDecimal netWeight, BigDecimal scrapPct) {
        MaterialMaster material = materialMasterRepository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(tenantAccessGuard.requireCustomerId(), materialCode)
                .orElseThrow(() -> ResourceNotFoundException.of("MaterialMaster", materialCode));

        PricingBasis pricingBasis;
        BigDecimal unitRate;
        if (material.isIndexLinkedFlag()) {
            pricingBasis = PricingBasis.INDEX_LINKED;
            var indexValue = commodityIndexService.getEffectiveValue(material.getIndexCode(), LocalDate.now());
            unitRate = MaterialCostEngine.computeIndexLinkedRate(
                    material.getBasePrice(), indexValue.getIndexValue(), material.getAlloySurchargePerUnit());
        } else {
            pricingBasis = PricingBasis.FIXED;
            unitRate = material.getRate();
        }

        MaterialCostResult result = MaterialCostEngine.calculate(new MaterialCostInput(
                netWeight, scrapPct, unitRate, pricingBasis,
                material.getScrapRecoveryRate(), material.getMaterialForm()));

        return new CompareLine(materialCode, material.getGrade(), unitRate, pricingBasis, result.materialCost());
    }

    /** MAT-07: 24 months of historical price data per grade, sourced from MDM-02's version history. */
    @Transactional(readOnly = true)
    public PriceTrendResponse priceTrend(String materialCode) {
        LocalDate since = LocalDate.now().minusMonths(24);
        List<PriceTrendPoint> points = materialMasterRepository.findByCodeOrderByVersionNumberDesc(materialCode)
                .stream()
                .filter(m -> m.getEffectiveFrom() != null && !m.getEffectiveFrom().isBefore(since))
                .map(m -> new PriceTrendPoint(m.getEffectiveFrom(), m.getVersionNumber(), m.getRate()))
                .toList();
        return new PriceTrendResponse(materialCode, points);
    }

    /** MAT-08: flags a part whose material cost exceeds a configurable % of total part cost. */
    public HighMaterialCostCheckResponse checkHighMaterialCostRatio(HighMaterialCostCheckRequest request) {
        BigDecimal ratio = request.materialCost()
                .divide(request.totalPartCost(), 10, RoundingMode.HALF_UP);
        boolean flagged = ratio.compareTo(request.thresholdFraction()) > 0;
        return new HighMaterialCostCheckResponse(ratio.setScale(6, RoundingMode.HALF_UP), flagged);
    }

    /** SRS 4.3.5: manual net-weight entry requires supervisor approval. */
    private void requireManualEntryApprovalIfApplicable(CalculateRequest request) {
        if (request.netWeightSource() == NetWeightSource.MANUAL
                && (request.manualEntryApprovedBy() == null || request.manualEntryApprovedBy().isBlank())) {
            throw new BusinessRuleViolationException(
                    "netWeight was entered manually (not sourced from CAD/PLM) and requires a "
                            + "supervisor's approval before it can be used in a cost calculation.",
                    "SRS-4.3.5-manual-net-weight-approval");
        }
    }

    /**
     * SRS 4.3.5: "Scrap percentage defaults from the process master...but may be
     * overridden per part with justification." An explicit override without a
     * justification is rejected; no override falls back to the named process's default.
     */
    private BigDecimal resolveScrapPct(CalculateRequest request) {
        if (request.scrapPctOverride() != null) {
            if (request.scrapPctOverrideJustification() == null
                    || request.scrapPctOverrideJustification().isBlank()) {
                throw new BusinessRuleViolationException(
                        "scrapPctOverride was supplied without a justification - process-master "
                                + "default scrap% may only be overridden per part with a stated reason.",
                        "SRS-4.3.5-scrap-override-justification");
            }
            return request.scrapPctOverride();
        }
        if (request.processName() == null || request.processName().isBlank()) {
            // No override and no process to default from - zero scrap is a legitimate
            // input (not every part has a scrap allowance), so this is not an error.
            return BigDecimal.ZERO;
        }
        ProcessMaster process = processMasterRepository.findByProcessName(request.processName())
                .orElseThrow(() -> ResourceNotFoundException.of("ProcessMaster", request.processName()));
        return process.getDefaultScrapPct() == null ? BigDecimal.ZERO : process.getDefaultScrapPct();
    }

    private Response toResponse(MaterialCostCalculation c) {
        return new Response(c.getCalculationId(), c.getBomNodeId(), c.getMaterialCode(), c.getNetWeight(),
                c.getScrapPct(), c.getGrossWeight(), c.getUnitRate(), c.getPricingBasis(),
                c.getScrapRecoveryCredit(), c.getMaterialCost(), c.getCalculationBasis(), c.getCalculatedAt());
    }
}
