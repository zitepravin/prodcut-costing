package com.automotive.costing.rfq.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuoteShouldCostComparisonServiceTest {
    @Test
    void statusConstantsStable() {
        assertEquals("AVAILABLE", QuoteShouldCostComparisonService.AVAILABLE);
        assertEquals("NOT_AVAILABLE", QuoteShouldCostComparisonService.NOT_AVAILABLE);
    }
}
