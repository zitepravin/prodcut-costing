package com.automotive.costing.bom.engine;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.common.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;

/**
 * BOM cost roll-up (BOM-02; TDD Section 6). This class is the calculation-engine core
 * for M02 and is deliberately a plain, framework-independent Java class with no Spring
 * MVC / HTTP dependency, so it can be invoked synchronously from an interactive API call
 * or asynchronously from a batch recalculation job with identical behavior (TDD Section 6.1).
 *
 * Non-negotiables implemented here, per TDD Section 6.2:
 *  - Idempotent: identical inputs -> byte-identical outputs. No reliance on ambient state,
 *    no floating point (BigDecimal only), no dependence on input list ordering.
 *  - Topological order: children are always fully resolved before their parent is computed,
 *    via post-order traversal from a computed parent->children adjacency map - never assumes
 *    the input list's array order reflects hierarchy.
 *  - Every division is guarded against a zero/blank denominator with an explicit validation
 *    error (BOM roll-up itself is multiplication - unit cost x quantity-per - so the
 *    division risk here is in scrap-based unit cost adjustments a leaf resolver might
 *    perform; this class validates quantityPer is present and non-negative before use,
 *    per BOM-05).
 *
 * SRS 4.2.5 business rule: "A BOM node's cost is always the sum of its direct children's
 * extended cost (unit cost x quantity-per), never manually overridden without an explicit
 * flag and justification note." This engine honors overrideFlag: if set (and a
 * justification is present - enforced by BomNodeService before the node ever reaches this
 * engine), the manual override value is used as that node's contribution instead of the
 * computed sum, but the override never propagates silently - callers can see exactly which
 * nodes were overridden via RollupResult.overriddenNodeIds().
 */
public final class BomRollupEngine {

    private BomRollupEngine() {
    }

    /**
     * Resolves the unit cost of a LEAF node (no children) from an external cost source.
     * M02 does not itself compute material/process/should-cost - that's M03/M04/M08's job.
     * This seam is exactly where those modules plug in once built; until then, callers pass
     * a resolver that falls back to zero (documented limitation, tracked in TASKS.md) or to
     * BomNode.manualCostOverride if the engineer has entered a placeholder cost.
     */
    @FunctionalInterface
    public interface LeafUnitCostResolver extends Function<BomNode, BigDecimal> {
    }

    public record RollupResult(
            Map<String, BigDecimal> rolledUpCostByNodeId,
            Set<String> overriddenNodeIds
    ) {
    }

    /**
     * Computes rolled-up extended cost for every node in {@code subtree}, bottom-up.
     * {@code subtree} must contain every descendant of the node(s) being rolled up (i.e.
     * the caller has already batch-loaded the full subtree per TDD Section 6.3 - this
     * method does not itself query the database).
     */
    public static RollupResult rollup(List<BomNode> subtree, LeafUnitCostResolver leafUnitCostResolver) {
        Map<String, BomNode> nodesById = new HashMap<>();
        Map<String, List<BomNode>> childrenByParentId = new HashMap<>();

        for (BomNode node : subtree) {
            nodesById.put(node.getBomNodeId(), node);
            if (node.getParentNodeId() != null) {
                childrenByParentId.computeIfAbsent(node.getParentNodeId(), k -> new ArrayList<>()).add(node);
            }
        }

        Map<String, BigDecimal> rolledUpCostByNodeId = new HashMap<>();
        Set<String> overriddenNodeIds = new HashSet<>();
        Set<String> inProgress = new HashSet<>(); // cycle guard

        // Process every node via post-order DFS. A BOM tree should never contain a cycle,
        // but a corrupted parent_node_id could create one - inProgress detects and rejects
        // it explicitly rather than infinite-looping or stack-overflowing silently.
        for (BomNode node : subtree) {
            computeNode(node, nodesById, childrenByParentId, rolledUpCostByNodeId,
                    overriddenNodeIds, inProgress, leafUnitCostResolver);
        }

        return new RollupResult(rolledUpCostByNodeId, overriddenNodeIds);
    }

    private static BigDecimal computeNode(BomNode node,
                                           Map<String, BomNode> nodesById,
                                           Map<String, List<BomNode>> childrenByParentId,
                                           Map<String, BigDecimal> memo,
                                           Set<String> overriddenNodeIds,
                                           Set<String> inProgress,
                                           LeafUnitCostResolver leafUnitCostResolver) {
        String nodeId = node.getBomNodeId();
        if (memo.containsKey(nodeId)) {
            return memo.get(nodeId);
        }
        if (inProgress.contains(nodeId)) {
            throw new BusinessRuleViolationException(
                    "Cycle detected in BOM tree at node " + nodeId + " - a node cannot be its own ancestor.",
                    "BOM-tree-integrity");
        }
        inProgress.add(nodeId);

        BigDecimal unitCost;
        if (node.isOverrideFlag() && node.getManualCostOverride() != null) {
            unitCost = node.getManualCostOverride();
            overriddenNodeIds.add(nodeId);
        } else {
            List<BomNode> children = childrenByParentId.getOrDefault(nodeId, List.of());
            if (children.isEmpty()) {
                // Leaf node: unit cost comes from the external cost source (M03/M08).
                unitCost = leafUnitCostResolver.apply(node);
                if (unitCost == null) {
                    unitCost = BigDecimal.ZERO;
                }
            } else {
                // SRS 4.2.5: node cost = sum of direct children's extended cost
                // (child unit cost x child quantity-per).
                BigDecimal sum = BigDecimal.ZERO;
                for (BomNode child : children) {
                    BigDecimal childUnitCost = computeNode(child, nodesById, childrenByParentId, memo,
                            overriddenNodeIds, inProgress, leafUnitCostResolver);
                    BigDecimal quantityPer = requireQuantityPer(child);
                    sum = sum.add(childUnitCost.multiply(quantityPer));
                }
                unitCost = sum;
            }
        }

        unitCost = unitCost.setScale(6, RoundingMode.HALF_UP);
        memo.put(nodeId, unitCost);
        inProgress.remove(nodeId);
        return unitCost;
    }

    /** BOM-05: quantity-per must be populated before roll-up executes. */
    private static BigDecimal requireQuantityPer(BomNode node) {
        if (node.getQuantityPer() == null) {
            throw new BusinessRuleViolationException(
                    "BOM node " + node.getBomNodeId() + " (part " + node.getPartNumber()
                            + ") is missing quantityPer - roll-up cannot execute until it is populated.",
                    "BOM-05");
        }
        if (node.getQuantityPer().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "BOM node " + node.getBomNodeId() + " has a negative quantityPer, which is invalid.",
                    "BOM-05");
        }
        return node.getQuantityPer();
    }
}
