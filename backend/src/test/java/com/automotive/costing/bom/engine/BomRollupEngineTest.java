package com.automotive.costing.bom.engine;

import com.automotive.costing.bom.engine.BomRollupEngine.RollupResult;
import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.entity.BomNodeStatus;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for BomRollupEngine against a HAND-CALCULATED CONTROL BOM, per Test Plan
 * Section 12 risk #4 ("BOM roll-up correctness at scale ... an error here propagates
 * silently through every downstream cost figure") and Section 8's E2E-01 scenario.
 * Corresponds to QA_Test_Case_Tracker.xlsx Module M02 (TC-0017 range, BOM-02).
 *
 * Control BOM structure:
 *
 *   ROOT (vehicle assembly)
 *   +-- A (sub-assembly), qtyPer=2
 *   |   +-- A1 (leaf), qtyPer=3, manual cost 5.00
 *   |   +-- A2 (leaf), qtyPer=1, manual cost 10.00
 *   +-- B (sub-assembly), qtyPer=1
 *       +-- B1 (leaf), qtyPer=4, manual cost 2.50
 *
 * Hand calculation:
 *   A1 unit cost = 5.00
 *   A2 unit cost = 10.00
 *   A  unit cost = (A1 * 3) + (A2 * 1) = 15.00 + 10.00 = 25.00
 *   B1 unit cost = 2.50
 *   B  unit cost = (B1 * 4) = 10.00
 *   ROOT unit cost = (A * 2) + (B * 1) = 50.00 + 10.00 = 60.00
 */
class BomRollupEngineTest {

    // Corresponds to TC-0017 (M02 / BOM-02, Positive): multi-level roll-up matches
    // the hand-calculated control BOM exactly.
    @Test
    void rollup_multiLevelTree_matchesHandCalculatedControlBom() {
        BomNode root = leafOrBranch("ROOT", null, null, null, null);
        BomNode a = leafOrBranch("A", "ROOT", new BigDecimal("2"), null, null);
        BomNode a1 = leafOrBranch("A1", "A", new BigDecimal("3"), new BigDecimal("5.00"), "test control value");
        BomNode a2 = leafOrBranch("A2", "A", new BigDecimal("1"), new BigDecimal("10.00"), "test control value");
        BomNode b = leafOrBranch("B", "ROOT", new BigDecimal("1"), null, null);
        BomNode b1 = leafOrBranch("B1", "B", new BigDecimal("4"), new BigDecimal("2.50"), "test control value");

        List<BomNode> subtree = List.of(root, a, a1, a2, b, b1);

        RollupResult result = BomRollupEngine.rollup(subtree, node -> BigDecimal.ZERO);

        assertThat(result.rolledUpCostByNodeId().get("A1")).isEqualByComparingTo("5.000000");
        assertThat(result.rolledUpCostByNodeId().get("A2")).isEqualByComparingTo("10.000000");
        assertThat(result.rolledUpCostByNodeId().get("A")).isEqualByComparingTo("25.000000");
        assertThat(result.rolledUpCostByNodeId().get("B1")).isEqualByComparingTo("2.500000");
        assertThat(result.rolledUpCostByNodeId().get("B")).isEqualByComparingTo("10.000000");
        assertThat(result.rolledUpCostByNodeId().get("ROOT")).isEqualByComparingTo("60.000000");

        assertThat(result.overriddenNodeIds()).containsExactlyInAnyOrder("A1", "A2", "B1");
    }

    // Corresponds to TC-0017 (repeat, idempotency clause): running the same roll-up twice
    // on freshly-built equal input produces byte-identical output (TDD Section 6.2).
    @Test
    void rollup_isIdempotent_acrossRepeatedRuns() {
        List<BomNode> subtree1 = buildControlBom();
        List<BomNode> subtree2 = buildControlBom();

        RollupResult result1 = BomRollupEngine.rollup(subtree1, node -> BigDecimal.ZERO);
        RollupResult result2 = BomRollupEngine.rollup(subtree2, node -> BigDecimal.ZERO);

        assertThat(result1.rolledUpCostByNodeId()).isEqualTo(result2.rolledUpCostByNodeId());
    }

    // Corresponds to TC-0018 (M02 / BOM-02, Negative/Boundary): a node missing quantityPer
    // must block roll-up with an explicit error, per BOM-05.
    @Test
    void rollup_withMissingQuantityPer_throwsBusinessRuleViolation() {
        BomNode root = leafOrBranch("ROOT", null, null, null, null);
        BomNode childMissingQty = new BomNode();
        childMissingQty.setBomNodeId("CHILD");
        childMissingQty.setParentNodeId("ROOT");
        childMissingQty.setPartNumber("CHILD-PART");
        childMissingQty.setQuantityPer(null); // BOM-05 violation
        childMissingQty.setStatus(BomNodeStatus.DRAFT);

        List<BomNode> subtree = List.of(root, childMissingQty);

        assertThatThrownBy(() -> BomRollupEngine.rollup(subtree, node -> BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("missing quantityPer");
    }

    // Confirms a corrupted parent_node_id forming a cycle is rejected explicitly rather
    // than infinite-looping (defense-in-depth beyond what the SRS explicitly requires).
    @Test
    void rollup_withCyclicParentReferences_throwsBusinessRuleViolation() {
        BomNode nodeX = leafOrBranch("X", "Y", new BigDecimal("1"), null, null);
        BomNode nodeY = leafOrBranch("Y", "X", new BigDecimal("1"), null, null);

        assertThatThrownBy(() -> BomRollupEngine.rollup(List.of(nodeX, nodeY), node -> BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cycle detected");
    }

    private List<BomNode> buildControlBom() {
        BomNode root = leafOrBranch("ROOT", null, null, null, null);
        BomNode a = leafOrBranch("A", "ROOT", new BigDecimal("2"), null, null);
        BomNode a1 = leafOrBranch("A1", "A", new BigDecimal("3"), new BigDecimal("5.00"), "control");
        BomNode a2 = leafOrBranch("A2", "A", new BigDecimal("1"), new BigDecimal("10.00"), "control");
        BomNode b = leafOrBranch("B", "ROOT", new BigDecimal("1"), null, null);
        BomNode b1 = leafOrBranch("B1", "B", new BigDecimal("4"), new BigDecimal("2.50"), "control");
        return List.of(root, a, a1, a2, b, b1);
    }

    private BomNode leafOrBranch(String id, String parentId, BigDecimal quantityPer,
                                  BigDecimal manualOverrideCost, String justification) {
        BomNode node = new BomNode();
        node.setBomNodeId(id);
        node.setParentNodeId(parentId);
        node.setPartNumber(id + "-PART");
        node.setQuantityPer(quantityPer);
        node.setStatus(BomNodeStatus.DRAFT);
        if (manualOverrideCost != null) {
            node.setOverrideFlag(true);
            node.setManualCostOverride(manualOverrideCost);
            node.setOverrideJustification(justification);
        }
        return node;
    }
}
