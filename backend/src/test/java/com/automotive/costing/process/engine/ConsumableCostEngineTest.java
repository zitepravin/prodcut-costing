package com.automotive.costing.process.engine;

import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.process.engine.ConsumableCostEngine.ConsumableLine;
import com.automotive.costing.process.engine.ConsumableCostEngine.ConsumableType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsumableCostEngineTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 4, 1);

    @Test
    void singleConsumable() {
        List<CostComponentResult> r = ConsumableCostEngine.calculate(
                new ConsumableCostEngine.ConsumableCostInput("op-1", "bom-1",
                        List.of(new ConsumableLine(ConsumableType.WIRE, new BigDecimal("0.05"), "kg",
                                new BigDecimal("120"), "INR", "VENDOR", AS_OF)), AS_OF));
        assertEquals(1, r.size());
        assertEquals(0, new BigDecimal("6.000000").compareTo(r.get(0).amount()));
        assertEquals("CONSUMABLE_WIRE", r.get(0).componentCode());
    }

    @Test
    void multipleConsumables() {
        List<CostComponentResult> r = ConsumableCostEngine.calculate(
                new ConsumableCostEngine.ConsumableCostInput("op-1", "bom-1", List.of(
                        new ConsumableLine(ConsumableType.GAS, new BigDecimal("2"), "m3",
                                new BigDecimal("10"), "INR", "VENDOR", AS_OF),
                        new ConsumableLine(ConsumableType.COOLANT, new BigDecimal("0.1"), "L",
                                new BigDecimal("50"), "INR", "VENDOR", AS_OF)
                ), AS_OF));
        assertEquals(2, r.size());
        BigDecimal sum = r.stream().map(CostComponentResult::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("25.000000").compareTo(sum));
    }

    @Test
    void zeroQuantity_ok() {
        List<CostComponentResult> r = ConsumableCostEngine.calculate(
                new ConsumableCostEngine.ConsumableCostInput("op-1", null,
                        List.of(new ConsumableLine(ConsumableType.ABRASIVE, BigDecimal.ZERO, "kg",
                                new BigDecimal("5"), "USD", null, null)), AS_OF));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.get(0).amount()));
    }

    @Test
    void missingRate_throws() {
        assertThrows(BusinessRuleViolationException.class, () ->
                ConsumableCostEngine.calculate(new ConsumableCostEngine.ConsumableCostInput(
                        "op-1", null, List.of(new ConsumableLine(ConsumableType.LUBRICANT,
                        new BigDecimal("1"), "L", null, "USD", null, null)), AS_OF)));
    }
}
