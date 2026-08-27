package com.automotive.costing.emissions;

import com.automotive.costing.emissions.service.ShouldCostEmissionsService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class EmissionsPolicyTest {
    @Test
    void constants() {
        assertEquals("NOT_AVAILABLE", ShouldCostEmissionsService.NOT_AVAILABLE);
        assertEquals("AVAILABLE", ShouldCostEmissionsService.AVAILABLE);
    }

    @Test
    void materialCo2eFormula() {
        BigDecimal qty = new BigDecimal("2.500000");
        BigDecimal factor = new BigDecimal("1.800000");
        BigDecimal co2e = qty.multiply(factor).setScale(6, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("4.500000"), co2e);
    }
}
