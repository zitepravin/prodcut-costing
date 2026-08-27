package com.automotive.costing.erp.sap.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static com.automotive.costing.erp.sap.engine.SapShouldCostCompareEngine.*;
import static org.junit.jupiter.api.Assertions.*;

class SapShouldCostCompareEngineTest {

    @Test
    void both_totals_present_computes_variance() {
        CompareResult r = compare(
                new BigDecimal("120.00"),
                new BigDecimal("100.00"),
                Map.of("MATERIAL", new BigDecimal("70"), "PROCESS", new BigDecimal("50")),
                Map.of("MATERIAL", new BigDecimal("60"), "PROCESS", new BigDecimal("40"))
        );
        assertEquals(0, new BigDecimal("20.000000").compareTo(r.absoluteVariance()));
        assertEquals(AVAILABLE, r.totalStatus());
        assertEquals(2, r.components().size());
    }

    @Test
    void missing_sap_total_not_available() {
        CompareResult r = compare(
                null,
                new BigDecimal("100"),
                Map.of(),
                Map.of("MATERIAL", new BigDecimal("100"))
        );
        assertEquals(NOT_AVAILABLE, r.totalStatus());
        assertNull(r.absoluteVariance());
        assertTrue(r.dataQualityNotes().stream().anyMatch(n -> n.contains("SAP total")));
    }

    @Test
    void unmapped_component_side_marked_not_available() {
        CompareResult r = compare(
                new BigDecimal("100"),
                new BigDecimal("100"),
                Map.of("MATERIAL", new BigDecimal("100")),
                Map.of("MATERIAL", new BigDecimal("90"), "PROCESS", new BigDecimal("10"))
        );
        assertEquals(PARTIAL, r.totalStatus());
        assertTrue(r.components().stream()
                .anyMatch(c -> "PROCESS".equals(c.code()) && NOT_AVAILABLE.equals(c.status())));
    }

    @Test
    void never_treats_null_as_zero_in_variance() {
        CompareResult r = compare(
                null,
                null,
                Map.of(),
                Map.of()
        );
        assertEquals(NOT_AVAILABLE, r.totalStatus());
        assertNull(r.absoluteVariance());
    }
}
