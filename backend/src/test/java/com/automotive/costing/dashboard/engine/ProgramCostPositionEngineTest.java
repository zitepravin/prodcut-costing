package com.automotive.costing.dashboard.engine;

import com.automotive.costing.shouldcost.entity.ApprovalStatus;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProgramCostPositionEngineTest {

    private ShouldCostBuildUp bu(String node, String scenario, ApprovalStatus status, String total, Instant at) {
        ShouldCostBuildUp b = new ShouldCostBuildUp();
        b.setBuildUpId("id-" + node + "-" + scenario + "-" + status);
        b.setBomNodeId(node);
        b.setScenarioLabel(scenario);
        b.setApprovalStatus(status);
        b.setTotalCost(new BigDecimal(total));
        b.setMaterialCost(new BigDecimal(total).multiply(new BigDecimal("0.5")));
        b.setProcessCost(BigDecimal.ZERO);
        b.setToolingCost(BigDecimal.ZERO);
        b.setPackagingFreightCost(BigDecimal.ZERO);
        b.setOverheadCost(BigDecimal.ZERO);
        b.setSgaCost(BigDecimal.ZERO);
        b.setProfitMargin(BigDecimal.ZERO);
        b.setCalculatedAt(at);
        return b;
    }

    @Test
    void twoScenariosSameNode_onlyOneSelected() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-02-01T00:00:00Z");
        var list = List.of(
                bu("N1", "Alt", ApprovalStatus.DRAFT, "100", t1),
                bu("N1", "Current Design", ApprovalStatus.DRAFT, "200", t2)
        );
        var selected = ProgramCostPositionEngine.selectOnePerBomNode(list, ProgramCostPositionEngine.POLICY_DEFAULT);
        assertEquals(1, selected.size());
        assertEquals(0, new BigDecimal("200").compareTo(selected.get(0).getTotalCost()));
    }

    @Test
    void approvedWinsOverNewerDraft() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-06-01T00:00:00Z");
        var list = List.of(
                bu("N1", "Current Design", ApprovalStatus.APPROVED, "150", t1),
                bu("N1", "Current Design", ApprovalStatus.DRAFT, "999", t2)
        );
        var selected = ProgramCostPositionEngine.selectOnePerBomNode(list, ProgramCostPositionEngine.POLICY_DEFAULT);
        assertEquals(1, selected.size());
        assertEquals(ApprovalStatus.APPROVED, selected.get(0).getApprovalStatus());
        assertEquals(0, new BigDecimal("150").compareTo(selected.get(0).getTotalCost()));
    }

    @Test
    void approvedOnly_skipsDraftOnlyNodes() {
        var list = List.of(bu("N1", "Current Design", ApprovalStatus.DRAFT, "100", Instant.now()));
        var selected = ProgramCostPositionEngine.selectOnePerBomNode(list, ProgramCostPositionEngine.POLICY_APPROVED_ONLY);
        assertTrue(selected.isEmpty());
    }

    @Test
    void sumDrivers_singleNode() {
        var b = bu("N1", "Current Design", ApprovalStatus.APPROVED, "100", Instant.now());
        var sum = ProgramCostPositionEngine.sumDrivers(List.of(b));
        assertEquals(0, new BigDecimal("100.000000").compareTo(sum.total()));
    }

    @Test
    void variance_shouldAboveTarget() {
        var v = ProgramCostPositionEngine.variance(new BigDecimal("110"), new BigDecimal("100"));
        assertNotNull(v);
        assertEquals(0, new BigDecimal("10.000000").compareTo(v.absoluteVariance()));
        assertEquals(0, new BigDecimal("0.100000").compareTo(v.percentageVariance()));
    }

    @Test
    void variance_nullWhenTargetMissing() {
        assertNull(ProgramCostPositionEngine.variance(new BigDecimal("100"), null));
    }

    @Test
    void emptyCandidates_emptySelection() {
        assertTrue(ProgramCostPositionEngine.selectOnePerBomNode(List.of(), null).isEmpty());
    }
}
