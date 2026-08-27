package com.automotive.costing.autointel;

import com.automotive.costing.autointel.engine.CostImpactEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CostImpactEngineTest {

    @Test
    void unitAndAnnualDelta() {
        BigDecimal delta = CostImpactEngine.unitDelta(new BigDecimal("100"), new BigDecimal("110"));
        assertEquals(0, new BigDecimal("10.000000").compareTo(delta));
        assertEquals(0, new BigDecimal("100000.000000").compareTo(
                CostImpactEngine.annualProgramDelta(delta, new BigDecimal("10000"))));
    }

    @Test
    void simulate_materialUp10() {
        BigDecimal sim = CostImpactEngine.simulateUnitCost(
                new BigDecimal("100"),
                new BigDecimal("40"), new BigDecimal("30"), new BigDecimal("10"),
                new BigDecimal("10"), null, null, null);
        // mat 44 + proc 30 + tool 10 + residual 20 = 104
        assertEquals(0, new BigDecimal("104.000000").compareTo(sim));
    }

    @Test
    void volumeAdjust() {
        assertEquals(0, new BigDecimal("8000.000000").compareTo(
                CostImpactEngine.adjustVolume(new BigDecimal("10000"), new BigDecimal("-20"))));
    }
}
