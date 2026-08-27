package com.automotive.costing.configuration.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CostingDefaultsPolicyTest {
    @Test
    void systemFixedPolicies() {
        assertEquals("NOT_AVAILABLE", CostingDefaultsService.FX_MISSING_POLICY);
        assertEquals("BLOCK", CostingDefaultsService.RELEASE_INCOMPLETE_POLICY);
    }
}
