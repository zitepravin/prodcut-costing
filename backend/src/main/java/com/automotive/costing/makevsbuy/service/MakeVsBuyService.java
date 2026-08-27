package com.automotive.costing.makevsbuy.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertRequest;
import com.automotive.costing.currency.service.CurrencyService;
import com.automotive.costing.currency.service.CostCurrencyConverter;
import com.automotive.costing.bom.entity.MakeBuyFlag;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.CreateRequest;
import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.DecisionExportPackage;
import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.DecideRequest;
import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.LegResponse;
import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.SimulationResponse;
import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.SourcesRequest;
import com.automotive.costing.makevsbuy.engine.MakeVsBuyComparisonEngine;
import com.automotive.costing.makevsbuy.engine.MakeVsBuyComparisonEngine.LegView;
import com.automotive.costing.makevsbuy.engine.MakeVsBuyComparisonEngine.Recommendation;
import com.automotive.costing.makevsbuy.entity.MakeVsBuyLeg;
import com.automotive.costing.makevsbuy.entity.MakeVsBuySimulation;
import com.automotive.costing.makevsbuy.repository.MakeVsBuyLegRepository;
import com.automotive.costing.makevsbuy.repository.MakeVsBuySimulationRepository;
import com.automotive.costing.mixedcost.entity.MixedCostVersion;
import com.automotive.costing.mixedcost.repository.MixedCostVersionRepository;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.rfq.entity.SupplierQuote;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.repository.SupplierMasterRepository;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.vendorselection.entity.SupplierTcoCalculation;
import com.automotive.costing.vendorselection.repository.SupplierTcoCalculationRepository;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MakeVsBuyService {

    private final MakeVsBuySimulationRepository simulationRepository;
    private final MakeVsBuyLegRepository legRepository;
    private final BomNodeRepository bomNodeRepository;
    private final ShouldCostBuildUpRepository buildUpRepository;
    private final SupplierQuoteRepository quoteRepository;
    private final SupplierMasterRepository supplierMasterRepository;
    private final MixedCostVersionRepository mixedCostVersionRepository;
    private final SupplierTcoCalculationRepository tcoRepository;
    private final CurrencyService currencyService;
    private final CostCurrencyConverter costCurrencyConverter;
    private final AccessScopeService accessScopeService;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional(readOnly = true)
    public List<SimulationResponse> listByBomNode(String bomNodeId) {
        BomNode node = requireNode(bomNodeId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, node.getProgramId());
        return simulationRepository.findByBomNodeIdOrderByCreatedAtDesc(bomNodeId).stream()
                .map(s -> toResponse(s, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SimulationResponse get(String simulationId) {
        MakeVsBuySimulation s = requireSim(simulationId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, s.getProgramId());
        return toResponse(s, List.of());
    }

    @Transactional
    public SimulationResponse create(String bomNodeId, CreateRequest request) {
        BomNode node = requireNode(bomNodeId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, node.getProgramId());
        if (request.label() == null || request.label().isBlank()) {
            throw new IllegalArgumentException("label is required");
        }

        MakeVsBuySimulation s = new MakeVsBuySimulation();
        s.setSimulationId(UUID.randomUUID().toString());
        s.setCustomerId(tenantAccessGuard.requireCustomerId());
        s.setProgramId(node.getProgramId());
        s.setBomNodeId(bomNodeId);
        s.setLabel(request.label().trim());
        s.setStatus("DRAFT");
        s.setSelectedBuildUpId(request.selectedBuildUpId());
        s.setMixedCostId(request.mixedCostId());
        s.setNotes(request.notes());
        s.setReportingCurrency(costCurrencyConverter.getBaseCurrency());
        simulationRepository.save(s);

        rebuildLegs(s, request.selectedBuildUpId(), request.buyQuoteIds(), request.mixedCostId());
        return toResponse(s, List.of("Simulation created (DRAFT). Run Compare for draft recommendation."));
    }

    @Transactional
    public SimulationResponse updateSources(String simulationId, SourcesRequest request) {
        MakeVsBuySimulation s = requireSim(simulationId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, s.getProgramId());
        if ("DECIDED".equals(s.getStatus()) || "FROZEN".equals(s.getStatus())) {
            throw new IllegalArgumentException("Cannot change sources after DECIDED");
        }
        s.setSelectedBuildUpId(request.selectedBuildUpId());
        s.setMixedCostId(request.mixedCostId());
        s.setStatus("DRAFT");
        s.setRecommendationCode(null);
        s.setRecommendationSummary(null);
        s.setFrozenAt(null);
        simulationRepository.save(s);
        rebuildLegs(s, request.selectedBuildUpId(), request.buyQuoteIds(), request.mixedCostId());
        return toResponse(s, List.of("Sources updated"));
    }

    @Transactional
    public SimulationResponse compare(String simulationId) {
        MakeVsBuySimulation s = requireSim(simulationId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, s.getProgramId());
        if ("DECIDED".equals(s.getStatus())) {
            throw new IllegalArgumentException("Cannot re-compare a DECIDED simulation");
        }

        // Re-resolve snapshots from live sources then recommend
        rebuildLegs(s, s.getSelectedBuildUpId(),
                legRepository.findBySimulationIdOrderByLegSeqAsc(simulationId).stream()
                        .filter(l -> MakeVsBuyComparisonEngine.BUY.equals(l.getStrategyType()))
                        .map(MakeVsBuyLeg::getSourceRefId)
                        .toList(),
                s.getMixedCostId());

        List<MakeVsBuyLeg> legs = legRepository.findBySimulationIdOrderByLegSeqAsc(simulationId);
        List<LegView> views = legs.stream()
                .map(l -> new LegView(
                        l.getLegSeq(),
                        l.getStrategyType(),
                        l.getSourceRefId(),
                        l.getSupplierId(),
                        l.getUnitCostSnapshot(),
                        l.getCurrencySnapshot(),
                        l.getTcoUnitSnapshot(),
                        l.getTcoStatus(),
                        l.getEligibilityStatus(),
                        l.getEligibilityNotes(),
                        l.getDataStatus(),
                        l.getRiskSummary()
                ))
                .toList();

        Recommendation rec = MakeVsBuyComparisonEngine.recommend(views);
        s.setRecommendationCode(rec.recommendationCode());
        s.setRecommendationSummary(rec.summary());
        s.setStatus("COMPARED");
        s.setFrozenAt(Instant.now());
        simulationRepository.save(s);
        return toResponse(s, rec.notes());
    }

    @Transactional
    public SimulationResponse decide(String simulationId, DecideRequest request) {
        MakeVsBuySimulation s = requireSim(simulationId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, s.getProgramId());
        if (request.decisionCode() == null || request.decisionCode().isBlank()) {
            throw new IllegalArgumentException("decisionCode is required");
        }
        if (request.decisionReason() == null || request.decisionReason().isBlank()) {
            throw new IllegalArgumentException("decisionReason is required");
        }
        String code = request.decisionCode().trim().toUpperCase();
        if (!List.of("APPROVE_MAKE", "APPROVE_BUY", "APPROVE_MIXED", "DEFER", "REJECT").contains(code)) {
            throw new IllegalArgumentException("Invalid decisionCode");
        }
        if (!"COMPARED".equals(s.getStatus()) && !"DECIDED".equals(s.getStatus())) {
            throw new IllegalArgumentException("Run Compare before Decide");
        }

        s.setDecisionCode(code);
        s.setDecisionReason(request.decisionReason().trim());
        s.setDecidedAt(Instant.now());
        s.setStatus("DECIDED");
        if (s.getFrozenAt() == null) {
            s.setFrozenAt(Instant.now());
        }
        simulationRepository.save(s);
        return toResponse(s, List.of("Decision recorded. Optional: apply-to-bom to update MakeBuyFlag."));
    }

    @Transactional
    public SimulationResponse applyToBom(String simulationId) {
        MakeVsBuySimulation s = requireSim(simulationId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, s.getProgramId());
        if (!"DECIDED".equals(s.getStatus())) {
            throw new IllegalArgumentException("Apply to BOM only after DECIDED");
        }
        if (s.getDecisionCode() == null) {
            throw new IllegalArgumentException("No decision code");
        }
        String dc = s.getDecisionCode();
        if ("DEFER".equals(dc) || "REJECT".equals(dc)) {
            throw new IllegalArgumentException("Cannot apply DEFER/REJECT to BOM flag");
        }

        BomNode node = requireNode(s.getBomNodeId());
        MakeBuyFlag previous = node.getMakeBuyFlag();
        if ("APPROVE_MAKE".equals(dc)) {
            node.setMakeBuyFlag(MakeBuyFlag.MAKE);
        } else if ("APPROVE_BUY".equals(dc) || "APPROVE_MIXED".equals(dc)) {
            node.setMakeBuyFlag(MakeBuyFlag.BUY);
        } else {
            throw new IllegalArgumentException("Unsupported decision for BOM apply: " + dc);
        }
        bomNodeRepository.save(node);
        s.setBomFlagApplied(true);
        s.setBomFlagAppliedAt(Instant.now());
        simulationRepository.save(s);
        return toResponse(s, List.of(
                "MakeBuyFlag updated from " + previous + " to " + node.getMakeBuyFlag()
                        + " (simulation " + s.getSimulationId() + ")"
        ));
    }

    private void rebuildLegs(MakeVsBuySimulation s, String buildUpId, List<String> quoteIds, String mixedCostId) {
        legRepository.deleteBySimulationId(s.getSimulationId());
        int seq = 1;

        if (buildUpId != null && !buildUpId.isBlank()) {
            MakeVsBuyLeg make = new MakeVsBuyLeg();
            make.setSimulationId(s.getSimulationId());
            make.setLegSeq(seq++);
            make.setStrategyType(MakeVsBuyComparisonEngine.MAKE);
            make.setSourceRefId(buildUpId.trim());
            make.setTcoStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE);
            String tenantIdBu = tenantAccessGuard.requireCustomerId();
            ShouldCostBuildUp bu = buildUpRepository.findByCustomerIdAndBuildUpId(tenantIdBu, buildUpId.trim()).orElse(null);
            if (bu == null) {
                make.setDataStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE);
                make.setEligibilityStatus(MakeVsBuyComparisonEngine.ELIGIBLE);
                make.setEligibilityNotes("Should-cost build-up not found");
            } else if (!s.getBomNodeId().equals(bu.getBomNodeId())) {
                make.setDataStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE);
                make.setEligibilityStatus(MakeVsBuyComparisonEngine.NOT_ELIGIBLE);
                make.setEligibilityNotes("Build-up does not belong to this BOM node");
            } else {
                make.setUnitCostSnapshot(bu.getTotalCost());
                make.setCurrencySnapshot(costCurrencyConverter.getBaseCurrency());
                make.setDataStatus(MakeVsBuyComparisonEngine.AVAILABLE);
                make.setEligibilityStatus(MakeVsBuyComparisonEngine.ELIGIBLE);
                alignLegCurrency(make, s);
            }
            legRepository.save(make);
            s.setSelectedBuildUpId(buildUpId.trim());
        }

        if (quoteIds != null) {
            for (String qid : quoteIds) {
                if (qid == null || qid.isBlank()) continue;
                MakeVsBuyLeg buy = new MakeVsBuyLeg();
                buy.setSimulationId(s.getSimulationId());
                buy.setLegSeq(seq++);
                buy.setStrategyType(MakeVsBuyComparisonEngine.BUY);
                buy.setSourceRefId(qid.trim());
                buy.setTcoStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE); // TCO not wired from VS in v1
                String tenantIdQ = tenantAccessGuard.requireCustomerId();
                SupplierQuote q = quoteRepository.findByCustomerIdAndQuoteId(tenantIdQ, qid.trim()).orElse(null);
                if (q == null) {
                    buy.setDataStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE);
                    buy.setEligibilityStatus(MakeVsBuyComparisonEngine.UNKNOWN);
                    buy.setEligibilityNotes("Quote not found");
                } else {
                    buy.setSupplierId(q.getSupplierId());
                    buy.setUnitCostSnapshot(q.getUnitPrice());
                    buy.setCurrencySnapshot(q.getCurrencyCode());
                    buy.setDataStatus(q.getUnitPrice() != null
                            ? MakeVsBuyComparisonEngine.AVAILABLE
                            : MakeVsBuyComparisonEngine.NOT_AVAILABLE);
                    alignLegCurrency(buy, s);
                    // P3-02: attach latest complete VS TCO for supplier+BOM when available
                    applyTcoSnapshot(buy, q.getSupplierId(), s.getBomNodeId());
                    var sm = supplierMasterRepository.findByCustomerIdAndSupplierId(tenantIdQ, q.getSupplierId());
                    if (sm.isPresent()) {
                        SupplierStatus st = sm.get().getStatus();
                        if (st == SupplierStatus.BLOCKED || st == SupplierStatus.INACTIVE) {
                            buy.setEligibilityStatus(MakeVsBuyComparisonEngine.NOT_ELIGIBLE);
                            buy.setEligibilityNotes("Supplier status is " + st);
                        } else {
                            buy.setEligibilityStatus(MakeVsBuyComparisonEngine.ELIGIBLE);
                        }
                    } else {
                        buy.setEligibilityStatus(MakeVsBuyComparisonEngine.UNKNOWN);
                    }
                }
                legRepository.save(buy);
            }
        }

        if (mixedCostId != null && !mixedCostId.isBlank()) {
            MakeVsBuyLeg mix = new MakeVsBuyLeg();
            mix.setSimulationId(s.getSimulationId());
            mix.setLegSeq(seq);
            mix.setStrategyType(MakeVsBuyComparisonEngine.MIXED);
            mix.setSourceRefId(mixedCostId.trim());
            mix.setTcoStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE);
            MixedCostVersion mv = mixedCostVersionRepository.findById(mixedCostId.trim()).orElse(null);
            if (mv == null) {
                mix.setDataStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE);
                mix.setEligibilityNotes("Mixed-cost version not found");
                mix.setEligibilityStatus(MakeVsBuyComparisonEngine.UNKNOWN);
            } else if (!s.getBomNodeId().equals(mv.getBomNodeId())) {
                mix.setDataStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE);
                mix.setEligibilityStatus(MakeVsBuyComparisonEngine.NOT_ELIGIBLE);
                mix.setEligibilityNotes("Mixed-cost version does not belong to this BOM node");
            } else if (mv.getBlendedUnitTotal() != null && "AVAILABLE".equals(mv.getBlendedStatus())) {
                mix.setUnitCostSnapshot(mv.getBlendedUnitTotal());
                mix.setDataStatus(MakeVsBuyComparisonEngine.AVAILABLE);
                mix.setEligibilityStatus(MakeVsBuyComparisonEngine.ELIGIBLE);
            } else {
                mix.setDataStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE);
                mix.setEligibilityStatus(MakeVsBuyComparisonEngine.ELIGIBLE);
                mix.setEligibilityNotes("Mixed cost not AVAILABLE");
            }
            legRepository.save(mix);
            s.setMixedCostId(mixedCostId.trim());
        }

        simulationRepository.save(s);
    }



    /**
     * P3-10: convert leg unit cost into simulation reporting currency when FX rate exists.
     * On missing rate: keep original amount, mark data NOT_AVAILABLE, note FX gap (never invent rate).
     */

    @Transactional(readOnly = true)
    public DecisionExportPackage exportDecisionPackage(String simulationId) {
        MakeVsBuySimulation s = requireSim(simulationId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, s.getProgramId());
        SimulationResponse full = toResponse(s, List.of());
        List<String> notes = new ArrayList<>();
        notes.add("FACT: export is read-only reference for customer CPI/SAP process teams.");
        notes.add("System does not post into SAP CO.");
        if (!"DECIDED".equals(s.getStatus())) {
            notes.add("WARNING: simulation status is " + s.getStatus() + " (not DECIDED).");
        }
        return new DecisionExportPackage(
                "MAKE_VS_BUY_DECISION",
                s.getSimulationId(),
                s.getProgramId(),
                s.getBomNodeId(),
                s.getStatus(),
                s.getRecommendationCode(),
                s.getDecisionCode(),
                s.getDecisionReason(),
                s.getDecidedAt() != null ? s.getDecidedAt().toString() : null,
                s.isBomFlagApplied(),
                s.getReportingCurrency(),
                full.legs(),
                Instant.now().toString(),
                notes
        );
    }

    private void alignLegCurrency(MakeVsBuyLeg leg, MakeVsBuySimulation s) {
        if (leg.getUnitCostSnapshot() == null) {
            return;
        }
        String reporting = s.getReportingCurrency();
        if (reporting == null || reporting.isBlank()) {
            reporting = costCurrencyConverter.getBaseCurrency();
            s.setReportingCurrency(reporting);
        }
        reporting = reporting.trim().toUpperCase();
        String from = leg.getCurrencySnapshot();
        if (from == null || from.isBlank()) {
            from = costCurrencyConverter.getBaseCurrency();
            leg.setCurrencySnapshot(from);
        }
        from = from.trim().toUpperCase();
        if (from.equals(reporting)) {
            return;
        }
        try {
            java.time.LocalDate asOf = s.getCreatedAt() != null
                    ? s.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    : java.time.LocalDate.now();
            var resp = currencyService.convert(
                    new ConvertRequest(leg.getUnitCostSnapshot(), from, reporting, asOf));
            leg.setUnitCostSnapshot(resp.convertedAmount());
            leg.setCurrencySnapshot(reporting);
            String notes = leg.getEligibilityNotes();
            String fxNote = "FX " + from + "->" + reporting + " path=" + resp.conversionPath();
            leg.setEligibilityNotes(notes == null || notes.isBlank() ? fxNote : notes + "; " + fxNote);
        } catch (Exception ex) {
            leg.setDataStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE);
            String notes = leg.getEligibilityNotes();
            String fxNote = "FX rate DATA NOT AVAILABLE for " + from + "->" + reporting;
            leg.setEligibilityNotes(notes == null || notes.isBlank() ? fxNote : notes + "; " + fxNote);
        }
    }

    private void applyTcoSnapshot(MakeVsBuyLeg buy, String supplierId, String bomNodeId) {
        buy.setTcoStatus(MakeVsBuyComparisonEngine.NOT_AVAILABLE);
        buy.setTcoUnitSnapshot(null);
        if (supplierId == null || bomNodeId == null) {
            return;
        }
        var list = tcoRepository.findByBomNodeIdOrderByCalculatedAtDesc(bomNodeId);
        for (SupplierTcoCalculation tco : list) {
            if (!supplierId.equals(tco.getSupplierId())) {
                continue;
            }
            if (tco.isDataComplete() && tco.getTcoTotal() != null) {
                // Prefer unit TCO: if annual volume present, derive per-unit; else use total as unit-level snapshot
                if (tco.getAnnualVolume() != null
                        && tco.getAnnualVolume().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    buy.setTcoUnitSnapshot(
                            tco.getTcoTotal().divide(tco.getAnnualVolume(), 6, java.math.RoundingMode.HALF_UP));
                } else if (tco.getPurchasePrice() != null) {
                    // Fallback: TCO total when already unit-oriented purchase + adders stored as total unit
                    buy.setTcoUnitSnapshot(tco.getTcoTotal());
                } else {
                    buy.setTcoUnitSnapshot(tco.getTcoTotal());
                }
                buy.setTcoStatus(MakeVsBuyComparisonEngine.AVAILABLE);
            }
            break; // latest for this supplier on node
        }
    }

    private BomNode requireNode(String id) {
        String customerId = tenantAccessGuard.requireCustomerId();
        return bomNodeRepository.findByCustomerIdAndBomNodeId(customerId, id)
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", id));
    }

    private MakeVsBuySimulation requireSim(String id) {
        MakeVsBuySimulation s = simulationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("MakeVsBuySimulation", id));
        tenantAccessGuard.assertOwns(s.getCustomerId(), "MakeVsBuySimulation", id);
        return s;
    }

    private SimulationResponse toResponse(MakeVsBuySimulation s, List<String> notes) {
        List<LegResponse> legs = legRepository.findBySimulationIdOrderByLegSeqAsc(s.getSimulationId())
                .stream()
                .map(l -> new LegResponse(
                        l.getLegSeq(),
                        l.getStrategyType(),
                        l.getSourceRefId(),
                        l.getSupplierId(),
                        l.getUnitCostSnapshot(),
                        l.getCurrencySnapshot(),
                        l.getTcoUnitSnapshot(),
                        l.getTcoStatus(),
                        l.getEligibilityStatus(),
                        l.getEligibilityNotes(),
                        l.getRiskSummary(),
                        l.getDataStatus()
                ))
                .toList();
        return new SimulationResponse(
                s.getSimulationId(),
                s.getProgramId(),
                s.getBomNodeId(),
                s.getLabel(),
                s.getStatus(),
                s.getSelectedBuildUpId(),
                s.getMixedCostId(),
                s.getRecommendationCode(),
                s.getRecommendationSummary(),
                s.getDecisionCode(),
                s.getDecisionReason(),
                s.getDecidedBy(),
                s.getDecidedAt(),
                s.isBomFlagApplied(),
                s.getBomFlagAppliedAt(),
                s.getFrozenAt(),
                s.getNotes(),
                legs,
                notes != null ? notes : List.of()
        );
    }
}
