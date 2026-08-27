package com.automotive.costing.purchase.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SapPoLinkServiceTest {
    @Test
    void constantsStable() {
        assertEquals("NOT_LINKED", SapPoLinkService.NOT_LINKED);
        assertEquals("LINKED", SapPoLinkService.LINKED);
        assertEquals("CPI_CALLBACK", SapPoLinkService.SOURCE_CPI);
        assertEquals("MANUAL_ADMIN", SapPoLinkService.SOURCE_HUMAN);
    }
}
