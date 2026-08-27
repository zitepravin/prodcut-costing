package com.automotive.costing.variance;

import com.automotive.costing.variance.engine.UnifiedVarianceEngine;
import com.automotive.costing.variance.engine.UnifiedVarianceEngine.CostSnapshot;
import com.automotive.costing.variance.engine.UnifiedVarianceEngine.VarianceResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedVarianceEngineTest {

    @Test
    void compare_explainsMaterialDriver() {
        CostSnapshot baseline = new CostSnapshot(
                new BigDecimal("40"), new BigDecimal("30"), new BigDecimal("10"),
                new BigDecimal("5"), new BigDecimal("15"), new BigDecimal("0"), new BigDecimal("0"),
                new BigDecimal("100"));
        CostSnapshot comparison = new CostSnapshot(
                new BigDecimal("50"), new BigDecimal("30"), new BigDecimal("10"),
                new BigDecimal("5"), new BigDecimal("15"), new BigDecimal("0"), new BigDecimal("0"),
                new BigDecimal("110"));

        VarianceResult r = UnifiedVarianceEngine.compare(baseline, comparison, "QUOTE");
        assertEquals(0, new BigDecimal("10.000000").compareTo(r.absoluteVariance()));
        assertTrue(r.drivers().stream().anyMatch(d -> "MATERIAL".equals(d.driverCode())
                && d.absoluteVariance().compareTo(new BigDecimal("10.000000")) == 0));
        assertTrue(r.summary().contains("comparison higher"));
    }
}
