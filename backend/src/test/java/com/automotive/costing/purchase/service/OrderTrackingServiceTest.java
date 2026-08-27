package com.automotive.costing.purchase.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class OrderTrackingServiceTest {

    @Test
    void dataStatus_notAvailableWhenAsnOrQtyMissing() {
        assertEquals(OrderTrackingService.NOT_AVAILABLE,
                OrderTrackingService.computeDataStatus(null, BigDecimal.ONE));
        assertEquals(OrderTrackingService.NOT_AVAILABLE,
                OrderTrackingService.computeDataStatus(LocalDate.now(), null));
    }

    @Test
    void dataStatus_availableWhenBothPresent() {
        assertEquals(OrderTrackingService.AVAILABLE,
                OrderTrackingService.computeDataStatus(LocalDate.now(), new BigDecimal("5")));
    }

    @Test
    void constantsStable() {
        assertEquals("CPI", OrderTrackingService.SOURCE_CPI);
        assertEquals("NOT_AVAILABLE", OrderTrackingService.NOT_AVAILABLE);
    }
}
