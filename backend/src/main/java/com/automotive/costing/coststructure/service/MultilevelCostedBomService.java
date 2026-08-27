package com.automotive.costing.coststructure.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.coststructure.dto.CostStructureDtos.CostedBomNode;
import com.automotive.costing.coststructure.dto.CostStructureDtos.MultilevelCostedBomResponse;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A4 Multilevel Costed BOM — tree with quantity, unit/extended cost, make/buy, component buckets.
 * Sources: rolled_up_cost on nodes + latest should-cost build-up when present.
 */
@Service
@RequiredArgsConstructor
public class MultilevelCostedBomService {

    private static final int SCALE = 6;

    private final BomNodeRepository bomNodeRepository;
    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;

    @Transactional(readOnly = true)
    public MultilevelCostedBomResponse costedTree(String rootBomNodeId) {
        BomNode root = bomNodeRepository.findByBomNodeId(rootBomNodeId)
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", rootBomNodeId));
        List<BomNode> subtree = bomNodeRepository.findSubtree(rootBomNodeId);
        if (subtree.isEmpty()) {
            subtree = List.of(root);
        }

        Map<String, List<BomNode>> childrenByParent = subtree.stream()
                .filter(n -> n.getParentNodeId() != null)
                .collect(Collectors.groupingBy(BomNode::getParentNodeId));

        Map<String, ShouldCostBuildUp> latestSc = new HashMap<>();
        for (BomNode n : subtree) {
            shouldCostBuildUpRepository.findByBomNodeIdOrderByCalculatedAtDesc(n.getBomNodeId()).stream()
                    .findFirst()
                    .ifPresent(sc -> latestSc.put(n.getBomNodeId(), sc));
        }

        CostedBomNode tree = build(root, childrenByParent, latestSc);
        return new MultilevelCostedBomResponse(
                root.getBomNodeId(),
                root.getProgramId(),
                tree.extendedCost() != null ? tree.extendedCost() : BigDecimal.ZERO,
                tree
        );
    }

    private CostedBomNode build(
            BomNode node,
            Map<String, List<BomNode>> childrenByParent,
            Map<String, ShouldCostBuildUp> latestSc) {

        List<CostedBomNode> children = childrenByParent.getOrDefault(node.getBomNodeId(), List.of()).stream()
                .sorted(Comparator.comparing(BomNode::getPartNumber, Comparator.nullsLast(String::compareTo)))
                .map(c -> build(c, childrenByParent, latestSc))
                .toList();

        ShouldCostBuildUp sc = latestSc.get(node.getBomNodeId());
        BigDecimal unitCost;
        BigDecimal material = BigDecimal.ZERO;
        BigDecimal process = BigDecimal.ZERO;
        BigDecimal tooling = BigDecimal.ZERO;
        BigDecimal pack = BigDecimal.ZERO;
        BigDecimal oh = BigDecimal.ZERO;
        String source;

        if (sc != null) {
            unitCost = nz(sc.getTotalCost());
            material = nz(sc.getMaterialCost());
            process = nz(sc.getProcessCost());
            tooling = nz(sc.getToolingCost());
            pack = nz(sc.getPackagingFreightCost());
            oh = nz(sc.getOverheadCost());
            source = "SHOULD_COST:" + sc.getBuildUpId();
        } else if (node.getRolledUpCost() != null) {
            unitCost = node.getRolledUpCost();
            source = "BOM_ROLLED_UP";
        } else if (node.getManualCostOverride() != null && node.isOverrideFlag()) {
            unitCost = node.getManualCostOverride();
            source = "MANUAL_OVERRIDE";
        } else {
            unitCost = BigDecimal.ZERO;
            source = "DATA_NOT_AVAILABLE";
        }

        // If children exist and no SC, sum children extended as extended cost preference
        BigDecimal qty = node.getQuantityPer() != null ? node.getQuantityPer() : BigDecimal.ONE;
        BigDecimal extended;
        if (!children.isEmpty() && sc == null) {
            BigDecimal childSum = children.stream()
                    .map(c -> c.extendedCost() != null ? c.extendedCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            extended = childSum.multiply(qty).setScale(SCALE, RoundingMode.HALF_UP);
            if (unitCost.compareTo(BigDecimal.ZERO) == 0 && qty.compareTo(BigDecimal.ZERO) > 0) {
                unitCost = childSum.setScale(SCALE, RoundingMode.HALF_UP);
                source = "CHILD_ROLLUP";
            }
        } else {
            extended = unitCost.multiply(qty).setScale(SCALE, RoundingMode.HALF_UP);
        }

        return new CostedBomNode(
                node.getBomNodeId(),
                node.getPartNumber(),
                node.getRevision(),
                node.getParentNodeId(),
                qty.setScale(SCALE, RoundingMode.HALF_UP),
                node.getUom(),
                node.getMakeBuyFlag() != null ? node.getMakeBuyFlag().name() : null,
                node.getMaterialCode(),
                node.getScrapPct(),
                unitCost.setScale(SCALE, RoundingMode.HALF_UP),
                extended,
                material.setScale(SCALE, RoundingMode.HALF_UP),
                process.setScale(SCALE, RoundingMode.HALF_UP),
                tooling.setScale(SCALE, RoundingMode.HALF_UP),
                pack.setScale(SCALE, RoundingMode.HALF_UP),
                oh.setScale(SCALE, RoundingMode.HALF_UP),
                source,
                node.isOverrideFlag(),
                children
        );
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
