package com.automotive.costing.bom.service;

import com.automotive.costing.bom.dto.BomNodeDtos.*;
import com.automotive.costing.bom.engine.BomRollupEngine;
import com.automotive.costing.bom.engine.BomRollupEngine.RollupResult;
import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.entity.BomNodeStatus;
import com.automotive.costing.bom.entity.MakeBuyFlag;
import com.automotive.costing.rfq.service.SupplierQuoteService;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.currency.service.CostCurrencyConverter;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.material.repository.MaterialCostCalculationRepository;
import com.automotive.costing.process.repository.ProcessCostCalculationRepository;
import com.automotive.costing.tooling.repository.ToolingCostCalculationRepository;
import com.automotive.costing.versioning.entity.ChangeTrigger;
import com.automotive.costing.versioning.service.CostVersionService;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * M02 BOM & Product Structure Management (BOM-01..BOM-08, SRS Section 4.2).
 *
 * Leaf-node unit cost resolution now reads M03's {@code MaterialCostCalculation} output
 * (see {@link #resolveLeafUnitCost}) for any leaf with a linked {@code materialCode} -
 * material cost is one component of a full part cost, not the whole of it (process,
 * tooling, overhead, SG&amp;A, and margin are M04/M05/M06/M08's responsibility and still
 * pending), so this remains a documented partial resolution rather than a full should-cost
 * figure until those modules land. A leaf with no materialCode, or no calculation yet on
 * record, still falls back to manualCostOverride-or-zero exactly as before.
 */
@Service
@RequiredArgsConstructor
public class BomNodeService {

    private static final String ENTITY_TYPE = "BomNode";

    private final BomNodeRepository repository;
    private final AuditService auditService;
    private final MaterialCostCalculationRepository materialCostCalculationRepository;
    private final ProcessCostCalculationRepository processCostCalculationRepository;
    private final ToolingCostCalculationRepository toolingCostCalculationRepository;
    private final com.automotive.costing.platform.user.service.AccessScopeService accessScopeService;
    private final VehicleProgramRepository vehicleProgramRepository;
    private final CostCurrencyConverter costCurrencyConverter;
    private final SupplierQuoteService supplierQuoteService;
    private final CostVersionService costVersionService;
    private final TenantAccessGuard tenantAccessGuard;

    /**
     * BOM-01: unlimited levels via import-based or explicit child-add tree construction.
     * SEC-03: the reference implementation of program-level data segregation - see
     * {@code AccessScopeService}'s Javadoc for why this is the first call site rather than
     * a system-wide retrofit, and NEXT_AGENT.md for the follow-up to extend this pattern
     * to other modules' program-scoped writes/reads.
     */
    @Transactional
    public Response addChild(String parentNodeId, String programId, CreateChildRequest request) {
        assertProgramAccess(programId);
        if (parentNodeId != null) {
            requireOwned(parentNodeId);
        }

        BomNode node = new BomNode();
        node.setBomNodeId(UUID.randomUUID().toString());
        node.setParentNodeId(parentNodeId);
        node.setProgramId(programId);
        node.setCustomerId(tenantAccessGuard.requireCustomerId());
        node.setPartNumber(request.partNumber());
        node.setRevision(request.revision());
        node.setQuantityPer(request.quantityPer());
        node.setUom(request.uom());
        node.setMakeBuyFlag(request.makeBuyFlag());
        node.setEcnReference(request.ecnReference());
        node.setNetWeight(request.netWeight());
        node.setScrapPct(request.scrapPct());
        node.setMaterialCode(request.materialCode());
        node.setStatus(BomNodeStatus.DRAFT);
        node.setWhatIfBranchFlag(false);

        BomNode saved = repository.save(node);
        auditService.recordCreate(ENTITY_TYPE, saved.getBomNodeId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public Response updateNode(String bomNodeId, UpdateNodeRequest request) {
        BomNode node = requireOwned(bomNodeId);
        BomNode before = snapshot(node);

        if (request.revision() != null) node.setRevision(request.revision());
        if (request.quantityPer() != null) node.setQuantityPer(request.quantityPer());
        if (request.uom() != null) node.setUom(request.uom());
        if (request.makeBuyFlag() != null) node.setMakeBuyFlag(request.makeBuyFlag());
        if (request.ecnReference() != null) node.setEcnReference(request.ecnReference());
        if (request.netWeight() != null) node.setNetWeight(request.netWeight());
        if (request.scrapPct() != null) node.setScrapPct(request.scrapPct());
        if (request.materialCode() != null) node.setMaterialCode(request.materialCode());

        BomNode saved = repository.save(node);
        auditService.recordUpdate(ENTITY_TYPE, saved.getBomNodeId(), toResponse(before), toResponse(saved));
        return toResponse(saved);
    }

    /**
     * SRS 4.2.5: "BOM changes cannot be saved as 'Released' status without a linked
     * ECN reference."
     */
    @Transactional
    public Response release(String bomNodeId) {
        BomNode node = requireOwned(bomNodeId);

        if (node.getEcnReference() == null || node.getEcnReference().isBlank()) {
            throw new BusinessRuleViolationException(
                    "Cannot release BOM node " + bomNodeId + " without a linked ECN reference.",
                    "SRS-4.2.5-ecn-required-for-release");
        }

        BomNode before = snapshot(node);
        node.setStatus(BomNodeStatus.RELEASED);
        BomNode saved = repository.save(node);
        // VER-01 automated versioning: immutable snapshot on BOM release (ECN-linked)
        java.util.Map<String, Object> verSnap = new java.util.LinkedHashMap<>();
        verSnap.put("bomNodeId", saved.getBomNodeId());
        verSnap.put("programId", saved.getProgramId());
        verSnap.put("partNumber", saved.getPartNumber());
        verSnap.put("revision", saved.getRevision());
        verSnap.put("status", String.valueOf(saved.getStatus()));
        verSnap.put("makeBuyFlag", saved.getMakeBuyFlag() != null ? String.valueOf(saved.getMakeBuyFlag()) : null);
        verSnap.put("quantityPer", saved.getQuantityPer() != null ? saved.getQuantityPer().toPlainString() : null);
        verSnap.put("ecnReference", saved.getEcnReference());
        verSnap.put("materialCode", saved.getMaterialCode());
        costVersionService.snapshotAfterChange(
                CostVersionService.TYPE_BOM_NODE,
                saved.getBomNodeId(),
                ChangeTrigger.ECN,
                saved.getEcnReference(),
                "BOM node released",
                verSnap);
        auditService.recordUpdate(ENTITY_TYPE, saved.getBomNodeId(), toResponse(before), toResponse(saved));
        return toResponse(saved);
    }

    /**
     * SRS 4.2.5: manual cost override requires an explicit flag and a justification note.
     * The override takes effect on the next roll-up (BomRollupEngine honors overrideFlag).
     */
    @Transactional
    public Response applyManualOverride(String bomNodeId, ManualOverrideRequest request) {
        BomNode node = requireOwned(bomNodeId);
        BomNode before = snapshot(node);

        node.setOverrideFlag(true);
        node.setManualCostOverride(request.overrideCost());
        node.setOverrideJustification(request.justification());

        BomNode saved = repository.save(node);
        auditService.recordUpdate(ENTITY_TYPE, saved.getBomNodeId(), toResponse(before), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public Response clearManualOverride(String bomNodeId) {
        BomNode node = requireOwned(bomNodeId);
        BomNode before = snapshot(node);

        node.setOverrideFlag(false);
        node.setManualCostOverride(null);
        node.setOverrideJustification(null);

        BomNode saved = repository.save(node);
        auditService.recordUpdate(ENTITY_TYPE, saved.getBomNodeId(), toResponse(before), toResponse(saved));
        return toResponse(saved);
    }

    /**
     * BOM-02: recursively sums child extended costs (unit cost x quantity-per) up through
     * every parent level. Delegates the actual computation to BomRollupEngine (TDD Section
     * 6) after batch-loading the full subtree in one query (TDD Section 6.3).
     *
     * <p>Multi-currency: calculated leaf component costs (material / process / tooling) are
     * treated as denominated in {@code app.costing.base-currency} and converted into the
     * owning program's {@code targetCurrency} via {@link CostCurrencyConverter}. Manual
     * overrides are left as-is (assumed already in program currency).
     */
    @Transactional
    public RollupTriggerResponse triggerRollup(String rootNodeId) {
        return triggerRollup(rootNodeId, null);
    }

    /**
     * BOM roll-up with explicit costing/valuation date for FX selection.
     * When costingDate is null, system date is used only at this request boundary.
     */
    public RollupTriggerResponse triggerRollup(String rootNodeId, LocalDate costingDate) {
        List<BomNode> subtree = repository.findSubtree(rootNodeId);
        if (subtree.isEmpty()) {
            throw ResourceNotFoundException.of("BomNode", rootNodeId);
        }

        String programId = subtree.get(0).getProgramId();
        String targetCurrency = resolveProgramTargetCurrency(programId);
        LocalDate asOf = costingDate != null ? costingDate : LocalDate.now();

        RollupResult result = BomRollupEngine.rollup(
                subtree, leaf -> resolveLeafUnitCost(leaf, targetCurrency, asOf));

        for (BomNode node : subtree) {
            BigDecimal newCost = result.rolledUpCostByNodeId().get(node.getBomNodeId());
            node.setRolledUpCost(newCost);
        }
        repository.saveAll(subtree);

        BigDecimal rootCost = result.rolledUpCostByNodeId().get(rootNodeId);
        return new RollupTriggerResponse(
                rootNodeId, subtree.size(), result.overriddenNodeIds().size(), rootCost);
    }

    /**
     * MAT-05: "recalculate all dependent BOM and part costs automatically when a material
     * master rate is updated and published." Rolls up every root node in the program,
     * which is the simple, correct implementation the codebase runs today; TDD Section 6.3's
     * incremental "recompute only the path from leaf to root" optimization remains a
     * follow-up once performance testing (TASKS.md non-functional item) shows it's needed
     * at the 20,000-node scale.
     */
    @Transactional
    public List<RollupTriggerResponse> triggerRollupForProgram(String programId) {
        return triggerRollupForProgram(programId, null);
    }

    public List<RollupTriggerResponse> triggerRollupForProgram(String programId, LocalDate costingDate) {
        List<BomNode> roots = repository.findByProgramIdAndParentNodeIdIsNull(programId);
        return roots.stream().map(root -> triggerRollup(root.getBomNodeId(), costingDate)).toList();
    }

    /**
     * Resolves a leaf node's unit cost. Priority order:
     *  1. Manual override, if the engineer has explicitly set one (always wins - SRS 4.2.5).
     *  2. For a 'Buy' part (SRS 4.2.5: "'Buy' sources its unit cost from the supplier
     *     quotation/costing module"): zero for now - M09/M10 (RFQ/Supplier Costing) are
     *     Phase 2 and don't exist yet. A 'Buy' leaf must NOT fall through to material/
     *     process/tooling cost, since that would misrepresent an externally-sourced part's
     *     cost as an internally-built-up one.
     *  3. For a 'Make' part (or an unflagged leaf, treated as 'Make' by default - the more
     *     conservative choice, since defaulting a real leaf to zero cost is worse than
     *     defaulting it to whatever internal cost data exists): M03's latest material cost
     *     (if `materialCode` is linked) PLUS M04's latest process cost (if any
     *     `ProcessRouting` for this node has been calculated) PLUS M06's latest tooling
     *     amortization (if any `ToolingRecord` for this node has been calculated AND its
     *     `includedInPiecePrice` flag is true - TL-05: Customer-owned tooling is tracked
     *     separately, not folded into piece price). Any, all, or none may be present -
     *     this remains a documented PARTIAL should-cost figure until M05/M08 (overhead
     *     absorption into piece price, SG&amp;A, margin) exist; M05's overhead pool
     *     absorption rate is a distinct, not-yet-wired cost element - see
     *     OverheadPoolService's Javadoc for why it isn't folded in here yet.
     */
    private BigDecimal resolveLeafUnitCost(BomNode leaf, String targetCurrency, LocalDate costingDate) {
        if (leaf.isOverrideFlag() && leaf.getManualCostOverride() != null) {
            return leaf.getManualCostOverride();
        }
        if (leaf.getMakeBuyFlag() == MakeBuyFlag.BUY) {
            // M10: ACCEPTED supplier quote unit price (SRS 4.2.5). Zero when no accepted quote yet.
            LocalDate asOf = costingDate != null ? costingDate : LocalDate.now();
            return supplierQuoteService.acceptedUnitPriceForBomNode(leaf.getBomNodeId())
                    .map(price -> costCurrencyConverter.toProgramCurrency(price, targetCurrency, asOf))
                    .orElse(BigDecimal.ZERO);
        }

        BigDecimal materialCost = leaf.getMaterialCode() == null || leaf.getMaterialCode().isBlank()
                ? BigDecimal.ZERO
                : materialCostCalculationRepository.findFirstByBomNodeIdOrderByCalculatedAtDesc(leaf.getBomNodeId())
                        .map(calc -> calc.getMaterialCost())
                        .orElse(BigDecimal.ZERO);

        BigDecimal processCost = processCostCalculationRepository
                .findFirstByBomNodeIdOrderByCalculatedAtDesc(leaf.getBomNodeId())
                .map(calc -> calc.getTotalProcessCost())
                .orElse(BigDecimal.ZERO);

        BigDecimal toolingCost = toolingCostCalculationRepository
                .findFirstByBomNodeIdOrderByCalculatedAtDesc(leaf.getBomNodeId())
                .filter(calc -> calc.isIncludedInPiecePrice())
                .map(calc -> calc.getTotalAmortizedCostPerPiece())
                .orElse(BigDecimal.ZERO);

        BigDecimal baseUnitCost = materialCost.add(processCost).add(toolingCost);
        LocalDate asOf = costingDate != null ? costingDate : LocalDate.now();
        BigDecimal inProgramCurrency = costCurrencyConverter.toProgramCurrency(baseUnitCost, targetCurrency, asOf);
        return applyBomScrapAllowance(inProgramCurrency, leaf.getScrapPct());
    }

    /**
     * Resolves the program's target currency for multi-currency roll-up.
     * Returns null when the program is missing or has no targetCurrency configured
     * (CostCurrencyConverter then treats amounts as already in base / no conversion).
     */
    private String resolveProgramTargetCurrency(String programId) {
        if (programId == null || programId.isBlank()) {
            return null;
        }
        String customerId = tenantAccessGuard.requireCustomerId();
        return vehicleProgramRepository.findByCustomerIdAndProgramId(customerId, programId)
                .map(VehicleProgram::getTargetCurrency)
                .orElse(null);
    }

    /**
     * Applies BOM-level scrap allowance to a composed leaf unit cost.
     * Convention matches MaterialCostEngine / BomNode.scrapPct: fraction in [0, 1).
     * effectiveUnitCost = base / (1 - scrapPct). Null or zero scrap leaves base unchanged.
     */
    static BigDecimal applyBomScrapAllowance(BigDecimal baseUnitCost, BigDecimal scrapPct) {
        if (scrapPct == null || scrapPct.compareTo(BigDecimal.ZERO) == 0) {
            return baseUnitCost;
        }
        if (scrapPct.compareTo(BigDecimal.ZERO) < 0 || scrapPct.compareTo(BigDecimal.ONE) >= 0) {
            throw new BusinessRuleViolationException(
                    "BOM scrapPct must be a fraction in the range [0, 1) - got " + scrapPct,
                    "BOM-05");
        }
        return baseUnitCost.divide(
                BigDecimal.ONE.subtract(scrapPct), 6, java.math.RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Response getById(String bomNodeId) {
        return toResponse(requireOwned(bomNodeId));
    }

    /** GET /api/v1/bom/{programId}/tree - builds the nested tree from one batch-loaded query. */
    @Transactional(readOnly = true)
    public List<TreeNode> getTree(String programId) {
        assertProgramAccess(programId);
        List<BomNode> allNodes = repository.findFullTreeByProgram(programId);
        Map<String, List<BomNode>> childrenByParent = allNodes.stream()
                .filter(n -> n.getParentNodeId() != null)
                .collect(Collectors.groupingBy(BomNode::getParentNodeId));

        return allNodes.stream()
                .filter(n -> n.getParentNodeId() == null)
                .map(root -> buildTreeNode(root, childrenByParent))
                .toList();
    }

    /**
     * SEC-03: enforces program-level data segregation. No-op if there is no authenticated
     * user in context (e.g. an internal/system-triggered call, such as the MAT-05
     * recalculation listener) - scope enforcement is an API-boundary concern, not
     * something internal service-to-service calls should be blocked by.
     */
    private void assertProgramAccess(String programId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        vehicleProgramRepository.findByCustomerIdAndProgramId(customerId, programId)
                .orElseThrow(() -> ResourceNotFoundException.of("VehicleProgram", programId));
        var user = accessScopeService.currentUserOrNull();
        if (user != null) {
            accessScopeService.assertAccess(user, com.automotive.costing.platform.user.entity.ScopeType.PROGRAM, programId);
        }
    }

    private TreeNode buildTreeNode(BomNode node, Map<String, List<BomNode>> childrenByParent) {
        List<TreeNode> children = childrenByParent.getOrDefault(node.getBomNodeId(), List.of()).stream()
                .map(c -> buildTreeNode(c, childrenByParent))
                .toList();
        return new TreeNode(node.getBomNodeId(), node.getPartNumber(), node.getRevision(),
                node.getQuantityPer(), node.getUom(), node.getMakeBuyFlag(), node.getMaterialCode(),
                node.getStatus(), node.getRolledUpCost(), node.isOverrideFlag(), children);
    }

    /** BOM-04: creates a what-if branch (a full subtree copy) without altering the released BOM. */
    @Transactional
    public Response createWhatIfBranch(String sourceNodeId) {
        BomNode sourceCheck = requireOwned(sourceNodeId);
        String tenantId = sourceCheck.getCustomerId();
        List<BomNode> subtree = repository.findSubtree(sourceNodeId);
        if (subtree.isEmpty()) {
            throw ResourceNotFoundException.of("BomNode", sourceNodeId);
        }

        Map<String, String> oldToNewId = subtree.stream()
                .collect(Collectors.toMap(BomNode::getBomNodeId, n -> UUID.randomUUID().toString()));

        BomNode sourceRoot = subtree.stream()
                .filter(n -> n.getBomNodeId().equals(sourceNodeId))
                .findFirst().orElseThrow();
        String newParentIdForRoot = sourceRoot.getParentNodeId(); // keep same parent as the original root

        List<BomNode> copies = subtree.stream().map(original -> {
            BomNode copy = new BomNode();
            copy.setBomNodeId(oldToNewId.get(original.getBomNodeId()));
            copy.setParentNodeId(original.getBomNodeId().equals(sourceNodeId)
                    ? newParentIdForRoot
                    : oldToNewId.get(original.getParentNodeId()));
            copy.setProgramId(original.getProgramId());
            copy.setCustomerId(tenantId);
            copy.setPartNumber(original.getPartNumber());
            copy.setRevision(original.getRevision());
            copy.setQuantityPer(original.getQuantityPer());
            copy.setUom(original.getUom());
            copy.setMakeBuyFlag(original.getMakeBuyFlag());
            copy.setNetWeight(original.getNetWeight());
            copy.setScrapPct(original.getScrapPct());
            copy.setMaterialCode(original.getMaterialCode());
            copy.setStatus(BomNodeStatus.DRAFT); // what-if branches are never Released directly
            copy.setWhatIfBranchFlag(true);
            copy.setRolledUpCost(original.getRolledUpCost());
            return copy;
        }).toList();

        repository.saveAll(copies);
        BomNode newRoot = copies.stream()
                .filter(n -> n.getBomNodeId().equals(oldToNewId.get(sourceNodeId)))
                .findFirst().orElseThrow();

        auditService.recordCreate(ENTITY_TYPE, newRoot.getBomNodeId(),
                "what-if branch of " + sourceNodeId + " (" + copies.size() + " nodes copied)");
        return toResponse(newRoot);
    }

    /**
     * BOM-08: flattened single-level raw-material export for a subtree. Leaf status is
     * computed in-memory from the already batch-loaded subtree (one query total) rather
     * than a per-node findByParentNodeId lookup - the earlier per-node-query version of
     * this method would have been exactly the N+1 pattern TDD Section 6.3 warns against.
     */
    @Transactional(readOnly = true)
    public List<FlattenedLineItem> flatten(String rootNodeId) {
        List<BomNode> subtree = repository.findSubtree(rootNodeId);
        java.util.Set<String> parentIds = subtree.stream()
                .map(BomNode::getParentNodeId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        return subtree.stream()
                .filter(n -> !parentIds.contains(n.getBomNodeId())) // leaves: nobody in the subtree has them as parent
                .map(n -> new FlattenedLineItem(
                        n.getBomNodeId(), n.getPartNumber(), null, n.getQuantityPer(), n.getUom()))
                .toList();
    }


    /**
     * Tenant-safe load: only returns nodes owned by the current customer.
     */
    private BomNode requireOwned(String bomNodeId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        BomNode node = repository.findByCustomerIdAndBomNodeId(customerId, bomNodeId)
                .orElseThrow(() -> ResourceNotFoundException.of(ENTITY_TYPE, bomNodeId));
        tenantAccessGuard.assertOwns(node.getCustomerId(), ENTITY_TYPE, bomNodeId);
        return node;
    }

    private BomNode snapshot(BomNode source) {
        BomNode copy = new BomNode();
        copy.setBomNodeId(source.getBomNodeId());
        copy.setCustomerId(source.getCustomerId());
        copy.setParentNodeId(source.getParentNodeId());
        copy.setProgramId(source.getProgramId());
        copy.setPartNumber(source.getPartNumber());
        copy.setRevision(source.getRevision());
        copy.setQuantityPer(source.getQuantityPer());
        copy.setUom(source.getUom());
        copy.setMakeBuyFlag(source.getMakeBuyFlag());
        copy.setEcnReference(source.getEcnReference());
        copy.setNetWeight(source.getNetWeight());
        copy.setScrapPct(source.getScrapPct());
        copy.setMaterialCode(source.getMaterialCode());
        copy.setStatus(source.getStatus());
        copy.setWhatIfBranchFlag(source.isWhatIfBranchFlag());
        copy.setRolledUpCost(source.getRolledUpCost());
        copy.setOverrideFlag(source.isOverrideFlag());
        copy.setManualCostOverride(source.getManualCostOverride());
        copy.setOverrideJustification(source.getOverrideJustification());
        return copy;
    }

    private Response toResponse(BomNode n) {
        return new Response(n.getBomNodeId(), n.getParentNodeId(), n.getProgramId(), n.getPartNumber(),
                n.getRevision(), n.getQuantityPer(), n.getUom(), n.getMakeBuyFlag(), n.getEcnReference(),
                n.getNetWeight(), n.getScrapPct(), n.getMaterialCode(), n.getStatus(), n.isWhatIfBranchFlag(),
                n.getRolledUpCost(), n.isOverrideFlag(), n.getOverrideJustification(),
                n.getCreatedBy(), n.getCreatedAt(), n.getUpdatedBy(), n.getUpdatedAt());
    }
}
