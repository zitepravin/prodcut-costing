package com.automotive.costing.purchase.service;

import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.ClosePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.CreatePurchaseOrderRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.DecidePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.IssuePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.PoLineInput;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.PurchaseOrderResponse;
import com.automotive.costing.purchase.entity.PurchaseOrder;
import com.automotive.costing.purchase.entity.PurchaseOrderLine;
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

/**
 * End-to-end operational P2P cycle centered on Create PO.
 * Stops at CLOSED — not ERP GR/IR.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderP2pCycleTest {

    @Mock PurchaseOrderRepository orderRepository;
    @Mock private com.automotive.costing.platform.access.tenant.TenantAccessGuard tenantAccessGuard;
    @Mock private com.automotive.costing.common.outbox.DomainOutboxService domainOutboxService;
    @Mock PurchaseOrderLineRepository lineRepository;
    @Mock PurchaseRequestRepository purchaseRequestRepository;
    @Mock PurchaseRequestLineRepository purchaseRequestLineRepository;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;

    @InjectMocks PurchaseOrderService service;

    @org.junit.jupiter.api.BeforeEach void stubTenant(){ org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("tenant-p2p"); }

    private final AtomicReference<PurchaseOrder> stored = new AtomicReference<>();
    private final List<PurchaseOrderLine> lines = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CustomerContext.setCustomerId("tenant-p2p");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer-p2p", "n/a"));
        when(orderRepository.existsByCustomerIdAndPoNumber(anyString(), anyString())).thenReturn(false);
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            stored.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(orderRepository.findByPurchaseOrderIdAndCustomerId(anyString(), eq("tenant-p2p")))
                .thenAnswer(inv -> Optional.ofNullable(stored.get())
                        .filter(p -> p.getPurchaseOrderId().equals(inv.getArgument(0))));
        when(lineRepository.save(any(PurchaseOrderLine.class))).thenAnswer(inv -> {
            PurchaseOrderLine l = inv.getArgument(0);
            lines.removeIf(x -> x.getLineId().equals(l.getLineId()));
            lines.add(l);
            return l;
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
    void fullCycle_createPoThroughClosed() {
        CreatePurchaseOrderRequest create = new CreatePurchaseOrderRequest(
                null, null, "supplier-p2p", "P01", null, "USD",
                "NET30", "FOB", null, null, null, null,
                List.of(new PoLineInput("P2P-1", "Cycle part", new BigDecimal("3"), "EA",
                        new BigDecimal("10.00"), "USD", null, null, null, null, null, null))
        );

        PurchaseOrderResponse po = service.create(create);
        assertEquals(PurchaseOrderStatus.DRAFT, po.status());
        String id = po.purchaseOrderId();

        assertEquals(PurchaseOrderStatus.APPROVAL, service.submit(id).status());
        assertEquals(PurchaseOrderStatus.APPROVED,
                service.decide(id, new DecidePoRequest("APPROVE", "P2P approve")).status());
        assertEquals(PurchaseOrderStatus.ISSUED,
                service.issue(id, new IssuePoRequest("P2P-EXT-REF")).status());
        assertEquals("P2P-EXT-REF", service.get(id).externalPoReference());
        assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED,
                service.close(id, new ClosePoRequest("lot1", true)).status());
        assertEquals(PurchaseOrderStatus.CLOSED,
                service.close(id, new ClosePoRequest("done", false)).status());
        assertTrue(PurchaseOrderStatus.isTerminal(PurchaseOrderStatus.CLOSED));
    }
}
