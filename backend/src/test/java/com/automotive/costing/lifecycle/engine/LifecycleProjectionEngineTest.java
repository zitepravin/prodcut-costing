package com.automotive.costing.lifecycle.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LifecycleProjectionEngineTest {

    @Test
    void projectsYearTotalsAndCumulative() {
        var years = List.of(
                new LifecycleProjectionEngine.YearVolume(1, 2027, new BigDecimal("100")),
                new LifecycleProjectionEngine.YearVolume(2, 2028, new BigDecimal("200"))
        );
        // insensitive 10, tooling cash 300 over lifetime 300 → tooling 1 per unit → unit 11
        var r = LifecycleProjectionEngine.project(new BigDecimal("10"), new BigDecimal("300"), years);
        assertEquals("AVAILABLE", r.unitCostStatus());
        assertEquals(0, new BigDecimal("11.000000").compareTo(r.unitCostBaseline()));
        assertEquals(2, r.years().size());
        assertEquals(0, new BigDecimal("1100.000000").compareTo(r.years().get(0).yearTotalCost()));
        assertEquals(0, new BigDecimal("2200.000000").compareTo(r.years().get(1).yearTotalCost()));
        assertEquals(0, new BigDecimal("3300.000000").compareTo(r.years().get(1).cumulativeCost()));
    }

    @Test
    void missingBaseline_notAvailable() {
        var r = LifecycleProjectionEngine.project(null, null, List.of(
                new LifecycleProjectionEngine.YearVolume(1, null, new BigDecimal("10"))
        ));
        assertEquals("NOT_AVAILABLE", r.unitCostStatus());
        assertTrue(r.years().isEmpty());
    }

    @Test
    void zeroVolumeYear_zeroTotal() {
        var r = LifecycleProjectionEngine.project(new BigDecimal("5"), null, List.of(
                new LifecycleProjectionEngine.YearVolume(1, null, BigDecimal.ZERO)
        ));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.years().get(0).yearTotalCost()));
        assertEquals("NOT_AVAILABLE", r.toolingStatus());
    }

    @Test
    void toolingCashHelper() {
        var cash = LifecycleProjectionEngine.toolingCash(new BigDecimal("2"), new BigDecimal("100"));
        assertEquals(0, new BigDecimal("200.000000").compareTo(cash));
        assertNull(LifecycleProjectionEngine.toolingCash(null, new BigDecimal("100")));
    }
}
