package com.automotive.costing.bom.engine;

import com.automotive.costing.bom.entity.BomNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Non-functional: large BOM roll-up in memory (TASKS.md — 10k+ nodes).
 * Pure engine test — no Spring, no DB. Measures wall time for a synthetic tree.
 */
@Tag("performance")
class BomRollupEnginePerformanceTest {

    private static final int NODE_COUNT = 10_000;
    /** Soft budget for CI; not a hard SLA. */
    private static final long MAX_MILLIS = 5_000L;

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void rollupTenThousandNodesCompletesWithinBudget() {
        List<BomNode> subtree = buildWideTree(NODE_COUNT);
        BigDecimal leafCost = new BigDecimal("1.250000");

        long start = System.nanoTime();
        BomRollupEngine.RollupResult result = BomRollupEngine.rollup(
                subtree, leaf -> leafCost);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertEquals(NODE_COUNT, result.rolledUpCostByNodeId().size());
        assertTrue(result.rolledUpCostByNodeId().values().stream()
                        .allMatch(v -> v != null && v.compareTo(BigDecimal.ZERO) >= 0),
                "all rolled-up costs non-negative");
        assertTrue(elapsedMs < MAX_MILLIS,
                "10k-node roll-up took " + elapsedMs + " ms (budget " + MAX_MILLIS + " ms)");
        System.out.println("PERF BomRollupEngine 10k nodes: " + elapsedMs + " ms");
    }

    /**
     * Root + (n-1) leaf children — stresses fan-out at one level (common automotive
     * assembly pattern for hardware kits).
     */
    private static List<BomNode> buildWideTree(int n) {
        List<BomNode> nodes = new ArrayList<>(n);
        String rootId = UUID.randomUUID().toString();
        BomNode root = new BomNode();
        root.setBomNodeId(rootId);
        root.setProgramId("perf-program");
        root.setPartNumber("ROOT");
        root.setQuantityPer(BigDecimal.ONE);
        root.setUom("EA");
        root.setOverrideFlag(false);
        nodes.add(root);

        for (int i = 1; i < n; i++) {
            BomNode leaf = new BomNode();
            leaf.setBomNodeId(UUID.randomUUID().toString());
            leaf.setParentNodeId(rootId);
            leaf.setProgramId("perf-program");
            leaf.setPartNumber("PART-" + i);
            leaf.setQuantityPer(BigDecimal.ONE);
            leaf.setUom("EA");
            leaf.setOverrideFlag(false);
            nodes.add(leaf);
        }
        return nodes;
    }
}
