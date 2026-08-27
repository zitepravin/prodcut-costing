package com.automotive.costing.shouldcost.service;

import com.automotive.costing.emissions.service.ShouldCostEmissionsService;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.versioning.entity.ChangeTrigger;
import com.automotive.costing.versioning.service.CostVersionService;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.entity.OverheadPool;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.OverheadPoolRepository;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.material.repository.MaterialCostCalculationRepository;
import com.automotive.costing.process.repository.ProcessCostCalculationRepository;
import com.automotive.costing.shouldcost.dto.ShouldCostDtos.*;
import com.automotive.costing.shouldcost.engine.CostComponentVarianceEngine;
import com.automotive.costing.shouldcost.engine.ShouldCostEngine;
import com.automotive.costing.costingvariant.entity.CostingVariant;
import com.automotive.costing.costingvariant.service.CostingVariantService;
import com.automotive.costing.shouldcost.entity.CostComponentDefinition;
import com.automotive.costing.shouldcost.repository.CostComponentDefinitionRepository;
import com.automotive.costing.shouldcost.repository.ShouldCostQuoteComponentRepository;
import java.util.HashMap;
import com.automotive.costing.shouldcost.engine.ShouldCostEngine.FullBuildUpInput;
import com.automotive.costing.shouldcost.engine.ShouldCostEngine.FullBuildUpResult;
import com.automotive.costing.costingvariant.entity.CostingVariant;
import com.automotive.costing.costingvariant.service.CostingVariantService;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.costingvariant.entity.CostingVariant;
import com.automotive.costing.costingvariant.service.CostingVariantService;
import com.automotive.costing.shouldcost.entity.SgaMarginBenchmark;
import com.automotive.costing.costingvariant.entity.CostingVariant;
import com.automotive.costing.costingvariant.service.CostingVariantService;
import com.automotive.costing.shouldcost.entity.ParametricCostBenchmark;
import com.automotive.costing.shouldcost.repository.SgaMarginBenchmarkRepository;
import com.automotive.costing.shouldcost.repository.ParametricCostBenchmarkRepository;
import com.automotive.costing.tooling.repository.ToolingCostCalculationRepository;
import com.automotive.costing.currency.service.CostCurrencyConverter;
import com.automotive.costing.packaging.repository.PackagingLogisticsCostCalculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * M08 Should-Cost Modeling &amp; Cost Estimation Engine - the module that finally
 * assembles M03 (material), M04 (process), M06 (tooling), overhead absorption (M05's
 * previously-unconsumed OHD-03 rate), and new SG&amp;A/margin benchmarks into the SRS
 * Appendix D nine-element should-cost figure. See {@code ShouldCostBuildUp}'s Javadoc for
 * why this is architecturally different from every calculation service before it (SC-06's
 * multi-scenario coexistence).
 *
 * SRS 4.8.5 business rule: "Should-cost calculations must never reference actual
 * supplier-quoted prices as an input." This class deliberately has no dependency on any
 * supplier-quote-shaped repository - {@link #varianceAgainstQuote} accepts a quoted price
 * as a plain request parameter for *comparison*, which is SC-04's explicit, different
 * purpose, not an input to the should-cost calculation itself.
 */
@Service
@RequiredArgsConstructor
public class ShouldCostService {

    private static final String ENTITY_TYPE = "ShouldCostBuildUp";
    private static final String DEFAULT_SCENARIO_LABEL = "Current Design";
    /** SC-07: MVP default staleness threshold, pending client confirmation (same pattern as MaterialMasterService's 10% variance threshold). */
    private static final Duration STALENESS_THRESHOLD = Duration.ofDays(90);

    private final BomNodeRepository bomNodeRepository;
    private final AccessScopeService accessScopeService;
    private final VehicleProgramRepository vehicleProgramRepository;
    private final MaterialCostCalculationRepository materialCostCalculationRepository;
    private final ProcessCostCalculationRepository processCostCalculationRepository;
    private final ToolingCostCalculationRepository toolingCostCalculationRepository;
    private final OverheadPoolRepository overheadPoolRepository;
    private final SgaMarginBenchmarkRepository sgaMarginBenchmarkRepository;
    private final ParametricCostBenchmarkRepository parametricCostBenchmarkRepository;
    private final ShouldCostBuildUpRepository buildUpRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final ShouldCostEmissionsService shouldCostEmissionsService;
    private final AuditService auditService;
    private final CostVersionService costVersionService;
    private final CostCurrencyConverter costCurrencyConverter;
    private final PackagingLogisticsCostCalculationRepository packagingLogisticsCostCalculationRepository;
    private final CostComponentDefinitionRepository costComponentDefinitionRepository;
    private final ShouldCostQuoteComponentRepository shouldCostQuoteComponentRepository;
    private final CostingVariantService costingVariantService;

    /**
     * SC-01: generates and persists a full should-cost build-up. Always creates a NEW
     * row (never updates an existing one in place) - the same VER-01-style "never edit
     * in place" convention used everywhere else in this system, which incidentally also
     * means the SC-01/4.8.5 "locked once linked to a negotiation record" rule never needs
     * to block *this* method, only a hypothetical future edit-in-place path that doesn't
     * exist.
     */
    @Transactional
    public Response generate(GenerateRequest request) {
        BomNode bomNode = bomNodeRepository.findByCustomerIdAndBomNodeId(tenantAccessGuard.requireCustomerId(), request.bomNodeId())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", request.bomNodeId()));
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, bomNode.getProgramId());
        if (request.commodityFamily() != null) {
            accessScopeService.assertAccessForCurrentUser(ScopeType.COMMODITY, request.commodityFamily());
        }
        VehicleProgram program = vehicleProgramRepository.findByCustomerIdAndProgramId(tenantAccessGuard.requireCustomerId(), bomNode.getProgramId())
                .orElseThrow(() -> ResourceNotFoundException.of("VehicleProgram", bomNode.getProgramId()));

        String scenarioLabel = request.scenarioLabel() == null || request.scenarioLabel().isBlank()
                ? DEFAULT_SCENARIO_LABEL : request.scenarioLabel();
        String region = request.region() == null || request.region().isBlank()
                ? program.getSourcingRegion() : request.region();
        if (region == null || region.isBlank()) {
            throw new BusinessRuleViolationException(
                    "No region supplied and the owning program has no sourcingRegion configured - "
                            + "SG&A/margin benchmarks are region-specific and cannot be resolved.",
                    "SRS-4.8.5-region-required");
        }

        String targetCurrency = program.getTargetCurrency();
        LocalDate costingDate = request.costingDate() != null ? request.costingDate() : LocalDate.now();
        var materialFx = costCurrencyConverter.convertToProgramCurrency(
                materialCostCalculationRepository
                        .findFirstByBomNodeIdOrderByCalculatedAtDesc(bomNode.getBomNodeId())
                        .map(c -> c.getMaterialCost()).orElse(BigDecimal.ZERO),
                targetCurrency, costingDate);
        BigDecimal materialCost = materialFx.convertedAmount();
        BigDecimal processCost = costCurrencyConverter.toProgramCurrency(
                processCostCalculationRepository
                        .findFirstByBomNodeIdOrderByCalculatedAtDesc(bomNode.getBomNodeId())
                        .map(c -> c.getTotalProcessCost()).orElse(BigDecimal.ZERO),
                targetCurrency, costingDate);
        BigDecimal toolingCost = costCurrencyConverter.toProgramCurrency(
                toolingCostCalculationRepository
                        .findFirstByBomNodeIdOrderByCalculatedAtDesc(bomNode.getBomNodeId())
                        .filter(c -> c.isIncludedInPiecePrice())
                        .map(c -> c.getTotalAmortizedCostPerPiece()).orElse(BigDecimal.ZERO),
                targetCurrency, costingDate);
        BigDecimal packagingFreightCost = costCurrencyConverter.toProgramCurrency(
                packagingLogisticsCostCalculationRepository
                        .findFirstByBomNodeIdOrderByCalculatedAtDesc(bomNode.getBomNodeId())
                        .map(c -> c.getTotalPackagingFreightCost())
                        .orElse(BigDecimal.ZERO),
                targetCurrency, costingDate);

        String resolvedOverheadPool = request.overheadPoolCode();
        CostingVariant appliedVariant = null;
        if (request.costingVariantId() != null && !request.costingVariantId().isBlank()) {
            appliedVariant = costingVariantService.requireActiveForCosting(request.costingVariantId().trim());
            if ((resolvedOverheadPool == null || resolvedOverheadPool.isBlank())
                    && appliedVariant.getOverheadPoolCode() != null) {
                resolvedOverheadPool = appliedVariant.getOverheadPoolCode();
            }
        }

        final String overheadPoolKey = resolvedOverheadPool;
        BigDecimal overheadCost = BigDecimal.ZERO;
        if (overheadPoolKey != null && !overheadPoolKey.isBlank()) {
            OverheadPool pool = overheadPoolRepository
                    .findFirstByCodeOrderByVersionNumberDesc(overheadPoolKey)
                    .orElseThrow(() -> ResourceNotFoundException.of("OverheadPool", overheadPoolKey));
            BigDecimal allocationBaseQuantity = request.allocationBaseQuantity() == null
                    ? BigDecimal.ZERO : request.allocationBaseQuantity();
            overheadCost = costCurrencyConverter.toProgramCurrency(
                    ShouldCostEngine.calculateOverheadCost(allocationBaseQuantity, pool.getRate()),
                    targetCurrency, costingDate);
        }

        SgaMarginBenchmark benchmark = sgaMarginBenchmarkRepository
                .findFirstByCommodityFamilyAndRegionAndActiveTrue(request.commodityFamily(), region)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No active SG&A/margin benchmark configured for commodityFamily='"
                                + request.commodityFamily() + "', region='" + region + "' (SRS 4.8.5).",
                        "SRS-4.8.5-benchmark-required"));

        FullBuildUpResult result = ShouldCostEngine.calculateFullBuildUp(new FullBuildUpInput(
                materialCost, processCost, toolingCost, packagingFreightCost, overheadCost,
                benchmark.getSgaPct(), benchmark.getMarginPct()));

        boolean stale = isAnyCalculationStale(bomNode.getBomNodeId());

        ShouldCostBuildUp entity = new ShouldCostBuildUp();
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setBuildUpId(UUID.randomUUID().toString());
        entity.setBomNodeId(bomNode.getBomNodeId());
        entity.setProgramId(bomNode.getProgramId());
        if (appliedVariant != null) {
            entity.setCostingVariantId(appliedVariant.getVariantId());
        }
        entity.setScenarioLabel(scenarioLabel);
        entity.setMaterialCost(materialCost);
        entity.setProcessCost(processCost);
        entity.setToolingCost(toolingCost);
        entity.setPackagingFreightCost(packagingFreightCost);
        entity.setOverheadCost(overheadCost);
        entity.setSgaCost(result.sgaCost());
        entity.setProfitMargin(result.profitMargin());
        entity.setTotalCost(result.totalCost());
        entity.setStaleInputsFlag(stale);
        entity.setLocked(false);
        entity.setApprovalStatus(com.automotive.costing.shouldcost.entity.ApprovalStatus.DRAFT);
        String basis = result.calculationBasis();
        if (appliedVariant != null) {
            basis = (basis == null ? "" : basis + " | ")
                    + "Variant=" + appliedVariant.getVariantCode()
                    + "; valuation=" + appliedVariant.getValuationStrategy()
                    + "; matPrice=" + appliedVariant.getMaterialPriceSource()
                    + "; actPrice=" + appliedVariant.getActivityPriceSource()
                    + "; QS=" + appliedVariant.getQuantityStructureSource()
                    + "; lot=" + appliedVariant.getLotSize()
                    + "; scrap=" + appliedVariant.getScrapPct()
                    + "; yield=" + appliedVariant.getYieldPct();
            if (basis.length() > 2000) {
                basis = basis.substring(0, 2000);
            }
        }
        entity.setCalculationBasis(basis);
        entity.setCalculatedAt(Instant.now());
        entity.setFxSourceCurrency(materialFx.sourceCurrency());
        entity.setFxTargetCurrency(materialFx.targetCurrency());
        entity.setFxAppliedRate(materialFx.appliedRate());
        entity.setFxRateEffectiveDate(materialFx.rateEffectiveDate());
        entity.setFxAsOfDate(materialFx.asOfDate());
        entity.setFxConversionMethod(materialFx.conversionMethod());
        entity.setFxRateSource(materialFx.rateSource());

        ShouldCostBuildUp saved = buildUpRepository.save(entity);
        try {
            shouldCostEmissionsService.computeForBuildUp(saved.getBuildUpId());
        } catch (Exception ignored) {
            // Emissions optional — never fail money should-cost generate
        }
        java.util.Map<String, Object> genSnap = new java.util.LinkedHashMap<>();
        genSnap.put("buildUpId", saved.getBuildUpId());
        genSnap.put("bomNodeId", saved.getBomNodeId());
        genSnap.put("scenarioLabel", scenarioLabel);
        genSnap.put("approvalStatus", String.valueOf(saved.getApprovalStatus()));
        if (saved.getTotalCost() != null) {
            genSnap.put("totalCost", saved.getTotalCost().toPlainString());
        }
        costVersionService.snapshotAfterChange(
                CostVersionService.TYPE_SHOULD_COST,
                saved.getBuildUpId(),
                ChangeTrigger.MANUAL_REVISION,
                scenarioLabel,
                "Should-cost generate",
                genSnap);
        auditService.recordCreate(ENTITY_TYPE, saved.getBuildUpId(), toResponse(saved));
        return toResponse(saved);
    }

    /** SC-02: rapid parametric estimate - weight + material family + process family only. */
    @Transactional(readOnly = true)
    public ParametricResponse rapidParametric(ParametricRequest request) {
        ParametricCostBenchmark benchmark = parametricCostBenchmarkRepository
                .findFirstByMaterialFamilyAndProcessFamilyAndRegionAndActiveTrue(
                        request.materialFamily(), request.processFamily(), request.region())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No active parametric benchmark configured for materialFamily='"
                                + request.materialFamily() + "', processFamily='" + request.processFamily()
                                + "', region='" + request.region() + "' (SC-02).",
                        "SC-02-benchmark-required"));

        BigDecimal estimate = ShouldCostEngine.calculateParametricCost(request.netWeightKg(), benchmark.getCostPerKg());
        return new ParametricResponse(request.netWeightKg(), benchmark.getCostPerKg(), estimate);
    }

    /** SC-03: cost breakdown waterfall, in the SRS Appendix D element order. */
    @Transactional(readOnly = true)
    public WaterfallResponse waterfall(String buildUpId) {
        ShouldCostBuildUp b = getOrThrow(buildUpId);
        List<WaterfallPoint> points = List.of(
                new WaterfallPoint("Material", b.getMaterialCost()),
                new WaterfallPoint("Process", b.getProcessCost()),
                new WaterfallPoint("Tooling", b.getToolingCost()),
                new WaterfallPoint("Packaging & Freight", b.getPackagingFreightCost()),
                new WaterfallPoint("Overhead", b.getOverheadCost()),
                new WaterfallPoint("SG&A", b.getSgaCost()),
                new WaterfallPoint("Profit Margin", b.getProfitMargin()),
                new WaterfallPoint("Total", b.getTotalCost()));
        return new WaterfallResponse(buildUpId, points);
    }

    /** SC-04: variance between should-cost and a quoted (or otherwise externally supplied) price. */
    @Transactional(readOnly = true)
    public VarianceResponse varianceAgainstQuote(VarianceRequest request) {
        ShouldCostBuildUp b = getOrThrow(request.buildUpId());
        if (b.getTotalCost().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleViolationException(
                    "Cannot compute percentage variance against a zero should-cost total.", "SC-04");
        }
        BigDecimal absoluteVariance = request.quotedPrice().subtract(b.getTotalCost());
        BigDecimal percentageVariance = absoluteVariance
                .divide(b.getTotalCost(), 6, RoundingMode.HALF_UP);
        return new VarianceResponse(b.getTotalCost(), request.quotedPrice(), absoluteVariance, percentageVariance);
    }

    /** SC-06: every scenario saved for a part - "Current Design", "Lightweighted Alternative", etc. */
    @Transactional(readOnly = true)
    public List<Response> listScenarios(String bomNodeId) {
        BomNode node = bomNodeRepository.findByBomNodeId(bomNodeId)
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", bomNodeId));
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, node.getProgramId());
        return buildUpRepository.findByBomNodeIdOrderByCalculatedAtDesc(bomNodeId).stream()
                .map(this::toResponse).toList();
    }

    /** SC-05: compare should-cost across two or more regional/scenario variants side by side. */
    @Transactional(readOnly = true)
    public RegionalCompareResponse regionalCompare(RegionalCompareRequest request) {
        if (request.buildUpIds() == null || request.buildUpIds().size() < 2) {
            throw new BusinessRuleViolationException(
                    "At least two should-cost build-ups are required to compare.", "SC-05");
        }
        List<RegionalCompareLine> lines = request.buildUpIds().stream()
                .map(this::getOrThrow)
                .map(b -> new RegionalCompareLine(b.getBuildUpId(), b.getScenarioLabel(), b.getTotalCost()))
                .sorted(Comparator.comparing(RegionalCompareLine::totalCost))
                .toList();
        String lowest = lines.isEmpty() ? null : lines.get(0).buildUpId();
        return new RegionalCompareResponse(lines, lowest);
    }

    /**
     * SRS 4.8.5: "A should-cost estimate is locked (read-only) once linked to a formal
     * supplier negotiation record." The seam - nothing calls this automatically yet since
     * M09/M10 don't exist (see the class Javadoc and ShouldCostBuildUp's Javadoc). As of
     * the M14 session, {@code ShouldCostApprovalListener} DOES call this automatically on
     * workflow approval - this manual endpoint remains for the M09/M10-linkage case,
     * which is still a forward reference.
     */
    @Transactional
    public Response lock(String buildUpId) {
        ShouldCostBuildUp b = getOrThrow(buildUpId);
        // V228 governance: prevent invalid transitions (e.g. DRAFT/REJECTED → APPROVED without submit/review)
        var st = b.getApprovalStatus();
        if (st == com.automotive.costing.shouldcost.entity.ApprovalStatus.APPROVED && b.isLocked()) {
            throw new BusinessRuleViolationException(
                    "Should-cost is already APPROVED and locked", "GOV-TRANSITION-01");
        }
        if (st != null && st != com.automotive.costing.shouldcost.entity.ApprovalStatus.SUBMITTED
                && st != com.automotive.costing.shouldcost.entity.ApprovalStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    "Cannot lock/approve should-cost from status " + st
                            + " — must be SUBMITTED (workflow complete) or already APPROVED",
                    "GOV-TRANSITION-02");
        }
        b.setLocked(true);
        b.setApprovalStatus(com.automotive.costing.shouldcost.entity.ApprovalStatus.APPROVED);
        ShouldCostBuildUp saved = buildUpRepository.save(b);
        auditService.recordUpdate(ENTITY_TYPE, buildUpId, null, "locked=true, approvalStatus=APPROVED");
        java.util.Map<String, Object> snap = new java.util.LinkedHashMap<>();
        snap.put("buildUpId", saved.getBuildUpId());
        snap.put("approvalStatus", "APPROVED");
        if (saved.getTotalCost() != null) {
            snap.put("totalCost", saved.getTotalCost().toPlainString());
        }
        costVersionService.snapshotAfterChange(
                CostVersionService.TYPE_SHOULD_COST,
                saved.getBuildUpId(),
                ChangeTrigger.APPROVAL_BASELINE,
                buildUpId,
                "Should-cost approved / locked",
                snap);
        return toResponse(saved);
    }

    /** WF-03-adjacent: marks a build-up SUBMITTED once a workflow instance is created for it (see ShouldCostApprovalListener). */
    @Transactional
    public Response markSubmitted(String buildUpId) {
        ShouldCostBuildUp b = getOrThrow(buildUpId);
        var st = b.getApprovalStatus();
        if (st == com.automotive.costing.shouldcost.entity.ApprovalStatus.APPROVED
                || b.isLocked()) {
            throw new BusinessRuleViolationException(
                    "Cannot re-submit an APPROVED/locked should-cost without controlled revision",
                    "GOV-TRANSITION-03");
        }
        if (st != null && st != com.automotive.costing.shouldcost.entity.ApprovalStatus.DRAFT
                && st != com.automotive.costing.shouldcost.entity.ApprovalStatus.SUBMITTED) {
            throw new BusinessRuleViolationException(
                    "Cannot submit should-cost from status " + st, "GOV-TRANSITION-04");
        }
        b.setApprovalStatus(com.automotive.costing.shouldcost.entity.ApprovalStatus.SUBMITTED);
        ShouldCostBuildUp saved = buildUpRepository.save(b);
        auditService.recordUpdate(ENTITY_TYPE, buildUpId, null, "approvalStatus=SUBMITTED");
        return toResponse(saved);
    }

    /** WF-06/SRS 4.14.5: "a rejected cost record returns to 'Draft' status" - see ShouldCostApprovalListener. */
    @Transactional
    public Response markRejectedReturnToDraft(String buildUpId) {
        ShouldCostBuildUp b = getOrThrow(buildUpId);
        if (b.getApprovalStatus() == com.automotive.costing.shouldcost.entity.ApprovalStatus.APPROVED
                || b.isLocked()) {
            throw new BusinessRuleViolationException(
                    "Cannot return APPROVED/locked should-cost to DRAFT via rejection path",
                    "GOV-TRANSITION-05");
        }
        b.setApprovalStatus(com.automotive.costing.shouldcost.entity.ApprovalStatus.DRAFT);
        ShouldCostBuildUp saved = buildUpRepository.save(b);
        auditService.recordUpdate(ENTITY_TYPE, buildUpId, null,
                "approvalStatus=DRAFT (workflow rejected - SRS 4.14.5)");
        return toResponse(saved);
    }


    @Transactional(readOnly = true)
    public List<CostComponentDefinitionDto> listActiveComponents(String tenantId) {
        return costComponentDefinitionRepository.findActiveForTenant(tenantId).stream()
            .map(d -> new CostComponentDefinitionDto(d.getComponentId(), d.getComponentCode(), d.getDisplayName(),
                d.getSequenceNo(), d.getCategory(), d.getMapsToColumn(), d.isActive())).toList();
    }
    @Transactional(readOnly = true)
    public DeepVarianceResponse deepVariance(DeepVarianceRequest request) {
        ShouldCostBuildUp b = getOrThrow(request.buildUpId());
        var defs = costComponentDefinitionRepository.findActiveForTenant(null);
        var views = defs.stream().map(d -> new CostComponentVarianceEngine.ComponentDefinitionView(
            d.getComponentCode(), d.getDisplayName(), d.getSequenceNo(), d.getMapsToColumn())).toList();
        Map<String, BigDecimal> cols = new HashMap<>();
        cols.put("material_cost", b.getMaterialCost()); cols.put("process_cost", b.getProcessCost());
        cols.put("tooling_cost", b.getToolingCost()); cols.put("packaging_freight_cost", b.getPackagingFreightCost());
        cols.put("overhead_cost", b.getOverheadCost()); cols.put("sga_cost", b.getSgaCost());
        cols.put("profit_margin", b.getProfitMargin()); cols.put("total_cost", b.getTotalCost());
        Map<String, BigDecimal> quoteByCode = new HashMap<>();
        if (request.componentSplits() != null) {
            for (var sp : request.componentSplits()) {
                if (sp.componentCode() != null && sp.quotedAmount() != null) quoteByCode.put(sp.componentCode(), sp.quotedAmount());
            }
        }
        shouldCostQuoteComponentRepository.findByBuildUpId(b.getBuildUpId())
            .forEach(qc -> quoteByCode.putIfAbsent(qc.getComponentCode(), qc.getQuotedAmount()));
        var result = CostComponentVarianceEngine.calculate(views, cols, quoteByCode, request.quotedTotal());
        var dtos = result.components().stream().map(c -> new ComponentAmountDto(
            c.componentCode(), c.displayName(), c.sequenceNo(), c.shouldCostAmount(), c.quotedAmount(),
            c.absoluteVariance(), c.percentageVariance(), c.dataStatus())).toList();
        return new DeepVarianceResponse(b.getBuildUpId(), result.shouldCostTotal(), result.quotedTotal(),
            result.absoluteVarianceTotal(), result.percentageVarianceTotal(), dtos, result.topContributors(), result.explanation());
    }

    private ShouldCostBuildUp getOrThrow(String buildUpId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        ShouldCostBuildUp b = buildUpRepository.findByCustomerIdAndBuildUpId(customerId, buildUpId)
                .orElseThrow(() -> ResourceNotFoundException.of(ENTITY_TYPE, buildUpId));
        tenantAccessGuard.assertOwns(b.getCustomerId(), ENTITY_TYPE, buildUpId);
        return b;
    }

    /** SC-07: flags the build-up if any consumed material/process/tooling calculation predates the staleness threshold. */
    private boolean isAnyCalculationStale(String bomNodeId) {
        Instant threshold = Instant.now().minus(STALENESS_THRESHOLD);
        boolean materialStale = materialCostCalculationRepository
                .findFirstByBomNodeIdOrderByCalculatedAtDesc(bomNodeId)
                .map(c -> c.getCalculatedAt() != null && c.getCalculatedAt().isBefore(threshold)).orElse(false);
        boolean processStale = processCostCalculationRepository
                .findFirstByBomNodeIdOrderByCalculatedAtDesc(bomNodeId)
                .map(c -> c.getCalculatedAt() != null && c.getCalculatedAt().isBefore(threshold)).orElse(false);
        boolean toolingStale = toolingCostCalculationRepository
                .findFirstByBomNodeIdOrderByCalculatedAtDesc(bomNodeId)
                .map(c -> c.getCalculatedAt() != null && c.getCalculatedAt().isBefore(threshold)).orElse(false);
        return materialStale || processStale || toolingStale;
    }

    private Response toResponse(ShouldCostBuildUp b) {
        return new Response(b.getBuildUpId(), b.getBomNodeId(), b.getScenarioLabel(), b.getMaterialCost(),
                b.getProcessCost(), b.getToolingCost(), b.getPackagingFreightCost(), b.getOverheadCost(),
                b.getSgaCost(), b.getProfitMargin(), b.getTotalCost(), b.isStaleInputsFlag(), b.isLocked(),
                b.getApprovalStatus().name(), b.getCalculationBasis(), b.getCalculatedAt());
    }
}
