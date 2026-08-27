package com.automotive.costing.purchase.service;

import com.automotive.costing.purchase.entity.PurchaseOrderTracking;
import com.automotive.costing.purchase.entity.PurchaseRequestLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryPerformanceServiceTest {

    private final DeliveryPerformanceService service = new DeliveryPerformanceService(null, null, null, null);

    @Test
    void lateWhenAsnAfterNeedBy() {
        PurchaseOrderTracking t = new PurchaseOrderTracking();
        t.setTrackingId("tr1");
        t.setPurchaseRequestId("pr1");
        t.setPurchaseRequestLineId("ln1");
        t.setAsnDate(LocalDate.of(2026, 8, 20));
        t.setQtyShipped(new BigDecimal("10"));

        PurchaseRequestLine line = new PurchaseRequestLine();
        line.setLineId("ln1");
        line.setSupplierId("sup1");
        line.setNeedByDate(LocalDate.of(2026, 8, 15));
        line.setQuantity(new BigDecimal("10"));

        var sp = service.evaluate(t, Map.of("ln1", line));
        assertEquals(DeliveryPerformanceService.AVAILABLE, sp.dataStatus());
        assertEquals(5L, sp.delayDays());
        assertEquals(Boolean.FALSE, sp.onTimeComputed());
        assertEquals(Boolean.FALSE, sp.onTimeEffective());
    }

    @Test
    void notAvailableWhenAsnMissing() {
        PurchaseOrderTracking t = new PurchaseOrderTracking();
        t.setTrackingId("tr1");
        t.setPurchaseRequestId("pr1");
        t.setPurchaseRequestLineId("ln1");
        t.setQtyShipped(BigDecimal.ONE);

        PurchaseRequestLine line = new PurchaseRequestLine();
        line.setLineId("ln1");
        line.setNeedByDate(LocalDate.of(2026, 8, 15));
        line.setQuantity(BigDecimal.ONE);

        var sp = service.evaluate(t, Map.of("ln1", line));
        assertEquals(DeliveryPerformanceService.NOT_AVAILABLE, sp.dataStatus());
        assertNull(sp.delayDays());
        assertNull(sp.onTimeComputed());
    }

    @Test
    void humanOverrideWinsOverComputed() {
        PurchaseOrderTracking t = new PurchaseOrderTracking();
        t.setTrackingId("tr1");
        t.setPurchaseRequestId("pr1");
        t.setPurchaseRequestLineId("ln1");
        t.setAsnDate(LocalDate.of(2026, 8, 20));
        t.setQtyShipped(BigDecimal.ONE);
        t.setOnTimeOverride(true);
        t.setOnTimeOverrideReason("Carrier force majeure accepted");

        PurchaseRequestLine line = new PurchaseRequestLine();
        line.setLineId("ln1");
        line.setNeedByDate(LocalDate.of(2026, 8, 15));
        line.setQuantity(BigDecimal.ONE);

        var sp = service.evaluate(t, Map.of("ln1", line));
        assertEquals(Boolean.FALSE, sp.onTimeComputed());
        assertEquals(Boolean.TRUE, sp.onTimeEffective());
        assertTrue(sp.humanOverrideApplied());
    }
}
