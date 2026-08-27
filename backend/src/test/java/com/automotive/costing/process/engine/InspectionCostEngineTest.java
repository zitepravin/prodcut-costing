package com.automotive.costing.process.engine;

import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.process.engine.InspectionCostEngine.InspectionActivity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InspectionCostEngineTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 4, 1);

    @Test
    void normalInspection() {
        List<CostComponentResult> r = InspectionCostEngine.calculate(
                new InspectionCostEngine.InspectionCostInput("op-1", "bom-1",
                        List.of(new InspectionActivity("a1", new BigDecimal("0.1"),
                                new BigDecimal("500"), "INR", "LABOR_MASTER", AS_OF, null)), AS_OF));
        // 0.1 * 500 = 50
        assertEquals(0, new BigDecimal("50.000000").compareTo(r.get(0).amount()));
        assertEquals("INSPECTION", r.get(0).componentCode());
    }

    @Test
    void withFixedCharge() {
        List<CostComponentResult> r = InspectionCostEngine.calculate(
                new InspectionCostEngine.InspectionCostInput("op-1", null,
                        List.of(new InspectionActivity("a1", new BigDecimal("0.1"),
                                new BigDecimal("500"), "INR", null, null, new BigDecimal("25"))), AS_OF));
        // 50 + 25 = 75
        assertEquals(0, new BigDecimal("75.000000").compareTo(r.get(0).amount()));
    }

    @Test
    void zeroTime_onlyFixed() {
        List<CostComponentResult> r = InspectionCostEngine.calculate(
                new InspectionCostEngine.InspectionCostInput("op-1", null,
                        List.of(new InspectionActivity("a1", BigDecimal.ZERO,
                                new BigDecimal("500"), "INR", null, null, new BigDecimal("10"))), AS_OF));
        assertEquals(0, new BigDecimal("10.000000").compareTo(r.get(0).amount()));
    }

    @Test
    void missingRate_throws() {
        assertThrows(BusinessRuleViolationException.class, () ->
                InspectionCostEngine.calculate(new InspectionCostEngine.InspectionCostInput(
                        "op-1", null, List.of(new InspectionActivity("a1", new BigDecimal("0.1"),
                        null, "INR", null, null, null)), AS_OF)));
    }

    @Test
    void multipleActivities() {
        List<CostComponentResult> r = InspectionCostEngine.calculate(
                new InspectionCostEngine.InspectionCostInput("op-1", null, List.of(
                        new InspectionActivity("a1", new BigDecimal("0.1"), new BigDecimal("100"), "USD", null, null, null),
                        new InspectionActivity("a2", new BigDecimal("0.2"), new BigDecimal("100"), "USD", null, null, null)
                ), AS_OF));
        assertEquals(2, r.size());
        BigDecimal sum = r.stream().map(CostComponentResult::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("30.000000").compareTo(sum));
    }
}
