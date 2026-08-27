package com.automotive.costing.autointel.service;

import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.autointel.dto.AutoIntelDtos.*;
import com.automotive.costing.autointel.engine.CostImpactEngine;
import com.automotive.costing.autointel.entity.EcnCostImpact;
import com.automotive.costing.autointel.entity.WhatIfSimulation;
import com.automotive.costing.autointel.repository.EcnCostImpactRepository;
import com.automotive.costing.autointel.repository.WhatIfSimulationRepository;
import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * E3 ECN cost impact · E4 central what-if.
 * Never invents baseline costs — requires should-cost or explicit inputs.
 */
@Service
@RequiredArgsConstructor
public class AutomotiveIntelService {

    private static final int SCALE = 6;

    private final EcnCostImpactRepository ecnRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final WhatIfSimulationRepository whatIfRepository;
    private final BomNodeRepository bomNodeRepository;
    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;

    // --- E3 ECN ---

    @Transactional(readOnly = true)
    public List<EcnImpactResponse> listEcnByBom(String bomNodeId) {
        return ecnRepository.findByBomNodeIdOrderByCreatedAtDesc(bomNodeId).stream()
                .map(this::toEcn).toList();
    }

    @Transactional
    public EcnImpactResponse analyzeEcn(AnalyzeEcnRequest req) {
        BomNode node = bomNodeRepository.findByBomNodeId(req.bomNodeId().trim())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", req.bomNodeId()));

        ShouldCostBuildUp baseline = resolveBuildUp(req.baselineBuildUpId(), node.getBomNodeId());
        if (baseline.getTotalCost() == null) {
            throw new BusinessRuleViolationException(
                    "Baseline should-cost has no total — cannot analyze ECN impact", "ECN_NO_BASELINE");
        }

        BigDecimal proposed;
        String proposedId = null;
        if (req.proposedBuildUpId() != null && !req.proposedBuildUpId().isBlank()) {
            ShouldCostBuildUp prop = shouldCostBuildUpRepository.findByCustomerIdAndBuildUpId(tenantAccessGuard.requireCustomerId(), req.proposedBuildUpId().trim())
                    .orElseThrow(() -> ResourceNotFoundException.of("ShouldCostBuildUp", req.proposedBuildUpId()));
            proposed = prop.getTotalCost();
            proposedId = prop.getBuildUpId();
            if (proposed == null) {
                throw new BusinessRuleViolationException("Proposed should-cost has no total", "ECN_NO_PROPOSED");
            }
        } else if (req.proposedUnitCost() != null) {
            proposed = req.proposedUnitCost();
        } else {
            throw new BusinessRuleViolationException(
                    "Provide proposedBuildUpId or proposedUnitCost", "ECN_PROPOSED_REQUIRED");
        }

        BigDecimal unitDelta = CostImpactEngine.unitDelta(baseline.getTotalCost(), proposed);
        BigDecimal annualVol = req.annualVolume();
        BigDecimal annualDelta = CostImpactEngine.annualProgramDelta(unitDelta, annualVol);

        EcnCostImpact e = new EcnCostImpact();
        e.setImpactId(UUID.randomUUID().toString());
        e.setEcnNumber(req.ecnNumber().trim());
        e.setEcnTitle(trimToNull(req.ecnTitle()));
        e.setProgramId(req.programId() != null && !req.programId().isBlank()
                ? req.programId().trim() : node.getProgramId());
        e.setBomNodeId(node.getBomNodeId());
        e.setBaselineBuildUpId(baseline.getBuildUpId());
        e.setProposedBuildUpId(proposedId);
        e.setBaselineUnitCost(baseline.getTotalCost().setScale(SCALE, RoundingMode.HALF_UP));
        e.setProposedUnitCost(proposed.setScale(SCALE, RoundingMode.HALF_UP));
        e.setUnitDelta(unitDelta);
        e.setAnnualVolume(annualVol);
        e.setAnnualProgramDelta(annualDelta);
        e.setCurrencyCode("USD");
        e.setStatus("ANALYZED");
        e.setImpactSummary(buildEcnSummary(e.getEcnNumber(), unitDelta, annualDelta, annualVol));
        e.setNotes(trimToNull(req.notes()));
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return toEcn(ecnRepository.save(e));
    }

    // --- E4 What-if ---

    @Transactional(readOnly = true)
    public List<WhatIfResponse> listWhatIf(String bomNodeId) {
        return whatIfRepository.findByBomNodeIdOrderByCreatedAtDesc(bomNodeId).stream()
                .map(this::toWhatIf).toList();
    }

    @Transactional
    public WhatIfResponse runWhatIf(RunWhatIfRequest req) {
        BomNode node = bomNodeRepository.findByBomNodeId(req.bomNodeId().trim())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", req.bomNodeId()));

        ShouldCostBuildUp baseline = resolveBuildUp(req.baselineBuildUpId(), node.getBomNodeId());
        if (baseline.getTotalCost() == null) {
            throw new BusinessRuleViolationException(
                    "No baseline unit cost for what-if — generate should-cost first", "WIF_NO_BASELINE");
        }

        BigDecimal simulated = CostImpactEngine.simulateUnitCost(
                baseline.getTotalCost(),
                baseline.getMaterialCost(),
                baseline.getProcessCost(),
                baseline.getToolingCost(),
                req.materialPctChange(),
                req.processPctChange(),
                req.toolingPctChange(),
                req.fxPctChange()
        );
        BigDecimal unitDelta = CostImpactEngine.unitDelta(baseline.getTotalCost(), simulated);
        BigDecimal adjVolume = CostImpactEngine.adjustVolume(req.annualVolume(), req.volumePctChange());
        // annual impact uses adjusted volume × unit delta
        BigDecimal annualDelta = unitDelta != null && adjVolume != null
                ? unitDelta.multiply(adjVolume).setScale(SCALE, RoundingMode.HALF_UP)
                : CostImpactEngine.annualProgramDelta(unitDelta, req.annualVolume());

        StringBuilder basis = new StringBuilder("E4 what-if on should-cost ").append(baseline.getBuildUpId());
        appendPct(basis, "material", req.materialPctChange());
        appendPct(basis, "process", req.processPctChange());
        appendPct(basis, "tooling", req.toolingPctChange());
        appendPct(basis, "fx", req.fxPctChange());
        appendPct(basis, "volume", req.volumePctChange());

        WhatIfSimulation w = new WhatIfSimulation();
        w.setSimulationId(UUID.randomUUID().toString());
        w.setBomNodeId(node.getBomNodeId());
        w.setProgramId(req.programId() != null && !req.programId().isBlank()
                ? req.programId().trim() : node.getProgramId());
        w.setLabel(req.label() != null && !req.label().isBlank()
                ? req.label().trim() : "What-if " + Instant.now());
        w.setBaselineUnitCost(baseline.getTotalCost().setScale(SCALE, RoundingMode.HALF_UP));
        w.setBaselineCurrency("USD");
        w.setMaterialPctChange(req.materialPctChange());
        w.setProcessPctChange(req.processPctChange());
        w.setToolingPctChange(req.toolingPctChange());
        w.setFxPctChange(req.fxPctChange());
        w.setVolumePctChange(req.volumePctChange());
        w.setSimulatedUnitCost(simulated);
        w.setUnitDelta(unitDelta);
        w.setAnnualVolume(adjVolume != null ? adjVolume : req.annualVolume());
        w.setAnnualDelta(annualDelta);
        w.setCalculationBasis(basis.length() > 2000 ? basis.substring(0, 2000) : basis.toString());
        w.setStatus("COMPLETED");
        w.setCreatedAt(Instant.now());
        return toWhatIf(whatIfRepository.save(w));
    }

    private ShouldCostBuildUp resolveBuildUp(String explicitId, String bomNodeId) {
        if (explicitId != null && !explicitId.isBlank()) {
            return shouldCostBuildUpRepository.findByCustomerIdAndBuildUpId(tenantAccessGuard.requireCustomerId(), explicitId.trim())
                    .orElseThrow(() -> ResourceNotFoundException.of("ShouldCostBuildUp", explicitId));
        }
        List<ShouldCostBuildUp> list = shouldCostBuildUpRepository
                .findByBomNodeIdOrderByCalculatedAtDesc(bomNodeId);
        if (list.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No should-cost for BOM node — provide baselineBuildUpId", "NO_SHOULD_COST");
        }
        return list.get(0);
    }

    private static String buildEcnSummary(String ecn, BigDecimal unitDelta, BigDecimal annual, BigDecimal vol) {
        StringBuilder sb = new StringBuilder("ECN ").append(ecn).append(": unit delta ");
        sb.append(unitDelta != null ? unitDelta.toPlainString() : "DATA_NOT_AVAILABLE");
        if (annual != null && vol != null) {
            sb.append("; annual program impact ").append(annual.toPlainString())
                    .append(" at volume ").append(vol.toPlainString());
        } else {
            sb.append("; annual program impact DATA_NOT_AVAILABLE (provide annualVolume)");
        }
        return sb.toString();
    }

    private static void appendPct(StringBuilder sb, String name, BigDecimal pct) {
        if (pct != null) sb.append("; ").append(name).append(' ').append(pct.toPlainString()).append('%');
    }

    private EcnImpactResponse toEcn(EcnCostImpact e) {
        return new EcnImpactResponse(
                e.getImpactId(), e.getEcnNumber(), e.getEcnTitle(), e.getProgramId(), e.getBomNodeId(),
                e.getBaselineBuildUpId(), e.getProposedBuildUpId(),
                e.getBaselineUnitCost(), e.getProposedUnitCost(), e.getUnitDelta(),
                e.getAnnualVolume(), e.getAnnualProgramDelta(), e.getCurrencyCode(),
                e.getStatus(), e.getImpactSummary(), e.getNotes(), e.getCreatedAt()
        );
    }

    private WhatIfResponse toWhatIf(WhatIfSimulation w) {
        return new WhatIfResponse(
                w.getSimulationId(), w.getBomNodeId(), w.getProgramId(), w.getLabel(),
                w.getBaselineUnitCost(), w.getBaselineCurrency(),
                w.getMaterialPctChange(), w.getVolumePctChange(), w.getFxPctChange(),
                w.getProcessPctChange(), w.getToolingPctChange(),
                w.getSimulatedUnitCost(), w.getUnitDelta(),
                w.getAnnualVolume(), w.getAnnualDelta(),
                w.getCalculationBasis(), w.getStatus(), w.getCreatedAt()
        );
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
