package com.automotive.costing.mfgintel;

import com.automotive.costing.mfgintel.engine.CapacityUtilizationEngine;
import com.automotive.costing.mfgintel.engine.CapacityUtilizationEngine.UtilizationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CapacityUtilizationEngineTest {

    @Test
    void utilization_okUnderCapacity() {
        UtilizationResult r = CapacityUtilizationEngine.calculate(
                new BigDecimal("100"), BigDecimal.ONE,
                List.of(new BigDecimal("40"), new BigDecimal("30")));
        assertEquals(0, new BigDecimal("70.000000").compareTo(r.requiredHours()));
        assertEquals(0, new BigDecimal("30.000000").compareTo(r.residualHours()));
        assertEquals(0, new BigDecimal("70.000000").compareTo(r.utilizationPct()));
        assertEquals("OK", r.status());
    }

    @Test
    void utilization_overloaded() {
        UtilizationResult r = CapacityUtilizationEngine.calculate(
                new BigDecimal("100"), new BigDecimal("0.8"),
                List.of(new BigDecimal("90")));
        // effective = 80; required 90 → OVERLOADED
        assertEquals(0, new BigDecimal("80.000000").compareTo(r.effectiveHours()));
        assertEquals("OVERLOADED", r.status());
    }

    @Test
    void activityCost() {
        assertEquals(0, new BigDecimal("250.000000").compareTo(
                CapacityUtilizationEngine.activityCost(new BigDecimal("10"), new BigDecimal("25"))));
    }
}
