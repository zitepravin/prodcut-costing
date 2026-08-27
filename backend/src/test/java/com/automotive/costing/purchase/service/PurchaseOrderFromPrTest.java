package com.automotive.costing.purchase.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.CreateFromPurchaseRequestRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.PurchaseOrderResponse;
import com.automotive.costing.purchase.entity.PurchaseOrder;
import com.automotive.costing.purchase.entity.PurchaseOrderLine;
import com.automotive.costing.purchase.entity.PurchaseRequest;
import com.automotive.costing.purchase.entity.PurchaseRequestLine;
import com.automotive.costing.purchase.repository.PurchaseOrderLineRepository;
import com.automotive.costing.purchase.repository.PurchaseOrderRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestLineRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseOrderFromPrTest {

    @Mock PurchaseOrderRepository orderRepository;
    @Mock PurchaseOrderLineRepository lineRepository;
    @Mock PurchaseRequestRepository purchaseRequestRepository;
    @Mock PurchaseRequestLineRepository purchaseRequestLineRepository;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;

    @InjectMocks PurchaseOrderService service;

    private final AtomicReference<PurchaseOrder> stored = new AtomicReference<>();
    private final List<PurchaseOrderLine> lines = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CustomerContext.setCustomerId("tenant-1");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "n/a"));
        when(orderRepository.existsByCustomerIdAndPoNumber(anyString(), anyString())).thenReturn(false);
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            stored.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(lineRepository.save(any(PurchaseOrderLine.class))).thenAnswer(inv -> {
            lines.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(lineRepository.findByPurchaseOrderIdOrderByLineNoAsc(anyString()))
                .thenAnswer(inv -> new ArrayList<>(lines));
        doNothing().when(lineRepository).deleteByPurchaseOrderId(anyString());
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
        SecurityContextHolder.clearContext();
        stored.set(null);
        lines.clear();
    }

    @Test
    void createsPoFromApprovedPr() {
        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId("pr-1");
        pr.setCustomerId("tenant-1");
        pr.setStatus(PurchaseRequestStatus.APPROVED);
        pr.setReportingCurrency("USD");
        pr.setPlantCode("P01");
        pr.setPrNumber("PR-100");
        pr.setExternalPoReference("4500099999");
        when(purchaseRequestRepository.findById("pr-1")).thenReturn(Optional.of(pr));

        PurchaseRequestLine pl = new PurchaseRequestLine();
        pl.setLineId("prl-1");
        pl.setPurchaseRequestId("pr-1");
        pl.setMaterialCode("MAT-9");
        pl.setMaterialDescription("Bracket");
        pl.setQuantity(new BigDecimal("4"));
        pl.setUom("EA");
        pl.setUnitPrice(new BigDecimal("11.00"));
        pl.setCurrency("USD");
        when(purchaseRequestLineRepository.findByPurchaseRequestIdOrderByLineNoAsc("pr-1"))
                .thenReturn(List.of(pl));

        PurchaseOrderResponse po = service.createFromPurchaseRequest(
                "pr-1",
                new CreateFromPurchaseRequestRequest("supplier-9", null, null, null, null, null)
        );
        assertEquals(PurchaseOrderStatus.DRAFT, po.status());
        assertEquals("pr-1", po.purchaseRequestId());
        assertEquals("supplier-9", po.supplierId());
        assertEquals(1, po.lines().size());
        assertEquals("MAT-9", po.lines().get(0).materialCode());
        assertEquals("AVAILABLE", po.lines().get(0).dataStatus());
        assertEquals("4500099999", po.externalPoReference());
    }

    @Test
    void doesNotInventExternalPoReferenceWhenPrHasNone() {
        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId("pr-4");
        pr.setCustomerId("tenant-1");
        pr.setStatus(PurchaseRequestStatus.APPROVED);
        pr.setReportingCurrency("USD");
        when(purchaseRequestRepository.findById("pr-4")).thenReturn(Optional.of(pr));
        PurchaseRequestLine pl = new PurchaseRequestLine();
        pl.setLineId("prl-4");
        pl.setPurchaseRequestId("pr-4");
        pl.setQuantity(BigDecimal.ONE);
        pl.setUom("EA");
        pl.setUnitPrice(new BigDecimal("1.00"));
        pl.setCurrency("USD");
        when(purchaseRequestLineRepository.findByPurchaseRequestIdOrderByLineNoAsc("pr-4"))
                .thenReturn(List.of(pl));

        PurchaseOrderResponse po = service.createFromPurchaseRequest(
                "pr-4",
                new CreateFromPurchaseRequestRequest("supplier-1", null, null, null, null, null)
        );
        assertNull(po.externalPoReference());
    }

    @Test
    void rejectsDraftPr() {
        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId("pr-2");
        pr.setCustomerId("tenant-1");
        pr.setStatus(PurchaseRequestStatus.DRAFT);
        when(purchaseRequestRepository.findById("pr-2")).thenReturn(Optional.of(pr));

        assertThrows(BusinessRuleViolationException.class, () ->
                service.createFromPurchaseRequest("pr-2",
                        new CreateFromPurchaseRequestRequest("s1", null, null, null, null, null)));
    }

    @Test
    void rejectsCrossTenantPr() {
        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId("pr-3");
        pr.setCustomerId("other-tenant");
        pr.setStatus(PurchaseRequestStatus.APPROVED);
        when(purchaseRequestRepository.findById("pr-3")).thenReturn(Optional.of(pr));

        assertThrows(Exception.class, () ->
                service.createFromPurchaseRequest("pr-3",
                        new CreateFromPurchaseRequestRequest("s1", null, null, null, null, null)));
    }
}
