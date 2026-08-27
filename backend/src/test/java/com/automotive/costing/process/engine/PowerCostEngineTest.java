package com.automotive.costing.process.engine;

import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.process.engine.PowerCostEngine.PowerCostInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PowerCostEngineTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 4, 1);

    @Test
    void normalPowerCalculation() {
        CostComponentResult r = PowerCostEngine.calculate(new PowerCostInput(
                "op-1", new BigDecimal("10"), new BigDecimal("0.5"),
                new BigDecimal("8.50"), "INR", "PLANT_TARIFF", AS_OF, AS_OF, "bom-1"));
        // 10 * 0.5 * 8.50 = 42.50
        assertEquals(0, new BigDecimal("42.500000").compareTo(r.amount()));
        assertEquals("POWER", r.componentCode());
        assertEquals("PWR-01", r.formulaCode());
        assertEquals("PLANT_TARIFF", r.rateSource());
        assertEquals(AS_OF, r.rateEffectiveDate());
    }

    @Test
    void zeroRuntime_yieldsZero() {
        CostComponentResult r = PowerCostEngine.calculate(new PowerCostInput(
                "op-1", new BigDecimal("10"), BigDecimal.ZERO,
                new BigDecimal("8.50"), "INR", "TARIFF", AS_OF, AS_OF, null));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.amount()));
    }

    @Test
    void zeroKw_yieldsZero() {
        CostComponentResult r = PowerCostEngine.calculate(new PowerCostInput(
                "op-1", BigDecimal.ZERO, new BigDecimal("1"),
                new BigDecimal("8.50"), "INR", "TARIFF", AS_OF, AS_OF, null));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.amount()));
    }

    @Test
    void missingElectricityRate_throws() {
        assertThrows(BusinessRuleViolationException.class, () ->
                PowerCostEngine.calculate(new PowerCostInput(
                        "op-1", new BigDecimal("5"), new BigDecimal("1"),
                        null, "INR", null, null, AS_OF, null)));
    }

    @Test
    void negativeKw_throws() {
        assertThrows(BusinessRuleViolationException.class, () ->
                PowerCostEngine.calculate(new PowerCostInput(
                        "op-1", new BigDecimal("-1"), new BigDecimal("1"),
                        new BigDecimal("8"), "INR", null, null, AS_OF, null)));
    }

    @Test
    void negativeRuntime_throws() {
        assertThrows(BusinessRuleViolationException.class, () ->
                PowerCostEngine.calculate(new PowerCostInput(
                        "op-1", new BigDecimal("1"), new BigDecimal("-0.1"),
                        new BigDecimal("8"), "INR", null, null, AS_OF, null)));
    }
}
