package com.automotive.costing.purchase.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.CreatePurchaseOrderRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.DecidePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.ClosePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.IssuePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.PoLineInput;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.PurchaseOrderResponse;
import com.automotive.costing.purchase.entity.PurchaseOrder;
import com.automotive.costing.purchase.entity.PurchaseOrderLine;
import com.automotive.costing.purchase.repository.PurchaseOrderLineRepository;
import com.automotive.costing.purchase.repository.PurchaseOrderRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Offline smoke: create → submit → decide(APPROVE) → issue on PURCHASE tenant context.
 * No Docker / MySQL required.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PurchaseOrderLifecycleTest {

    private static final String TENANT = "tenant-purchase-001";
    private static final String USER = "buyer-user-1";

    @Mock PurchaseOrderRepository orderRepository;
    @Mock private com.automotive.costing.platform.access.tenant.TenantAccessGuard tenantAccessGuard;
    @Mock private com.automotive.costing.common.outbox.DomainOutboxService domainOutboxService;
    @Mock PurchaseOrderLineRepository lineRepository;
    @Mock PurchaseRequestRepository purchaseRequestRepository;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;

    @InjectMocks PurchaseOrderService service;

    @org.junit.jupiter.api.BeforeEach void stubTenant(){ org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn(TENANT); }

    private final AtomicReference<PurchaseOrder> stored = new AtomicReference<>();
    private final List<PurchaseOrderLine> lines = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CustomerContext.setCustomerId(TENANT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER, "n/a"));

        when(orderRepository.existsByCustomerIdAndPoNumber(anyString(), anyString())).thenReturn(false);
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            stored.set(po);
            return po;
        });
        when(orderRepository.findByPurchaseOrderIdAndCustomerId(anyString(), eq(TENANT)))
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

        User u = new User();
        // minimal — publish may look up user
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
    void createSubmitDecideIssue_happyPath() {
        CreatePurchaseOrderRequest create = new CreatePurchaseOrderRequest(
                null, null, "supplier-1", "P01", null, "USD",
                "NET30", "FOB", null, null, null, null,
                List.of(new PoLineInput("MAT-1", "Bracket", new BigDecimal("10"), "EA",
                        new BigDecimal("12.50"), "USD", null, null, null, "P01", null, null))
        );

        PurchaseOrderResponse draft = service.create(create);
        assertEquals(PurchaseOrderStatus.DRAFT, draft.status());
        assertNotNull(draft.purchaseOrderId());
        assertEquals(TENANT, draft.customerId());
        assertEquals(1, draft.lines().size());
        assertEquals("AVAILABLE", draft.lines().get(0).dataStatus());

        String id = draft.purchaseOrderId();

        PurchaseOrderResponse approval = service.submit(id);
        assertEquals(PurchaseOrderStatus.APPROVAL, approval.status());

        PurchaseOrderResponse approved = service.decide(id, new DecidePoRequest("APPROVE", "OK to buy"));
        assertEquals(PurchaseOrderStatus.APPROVED, approved.status());
        assertEquals("OK to buy", approved.decisionReason());

        PurchaseOrderResponse issued = service.issue(id, new IssuePoRequest("ERP-PO-999"));
        assertEquals(PurchaseOrderStatus.ISSUED, issued.status());
        assertEquals("ERP-PO-999", issued.externalPoReference());
        assertNotNull(issued.issuedAt());
    }

    @Test
    void issueBlockedWhenPriceNotAvailable() {
        CreatePurchaseOrderRequest create = new CreatePurchaseOrderRequest(
                null, null, "supplier-1", null, null, "USD",
                null, null, null, null, null, null,
                List.of(new PoLineInput("MAT-2", null, new BigDecimal("1"), "EA",
                        null, null, null, null, null, null, null, null))
        );
        PurchaseOrderResponse draft = service.create(create);
        assertEquals("NOT_AVAILABLE", draft.lines().get(0).dataStatus());

        service.submit(draft.purchaseOrderId());
        service.decide(draft.purchaseOrderId(), new DecidePoRequest("APPROVE", "approve anyway"));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.issue(draft.purchaseOrderId(), new IssuePoRequest(null)));
        String ref = ex.getRuleReference() == null ? "" : ex.getRuleReference();
        assertTrue(ex.getMessage().toLowerCase().contains("not_available")
                || ref.contains("NOT_AVAILABLE"));
    }

    @Test
    void requiresCustomerContext() {
        CustomerContext.clear();
        CreatePurchaseOrderRequest create = new CreatePurchaseOrderRequest(
                null, null, "supplier-1", null, null, "USD",
                null, null, null, null, null, null,
                List.of(new PoLineInput(null, null, BigDecimal.ONE, "EA",
                        BigDecimal.TEN, "USD", null, null, null, null, null, null))
        );
        assertThrows(BusinessRuleViolationException.class, () -> service.create(create));
    }

    @Test
    void closeAfterIssue_happyPath() {
        CreatePurchaseOrderRequest create = new CreatePurchaseOrderRequest(
                null, null, "supplier-1", "P01", null, "USD",
                "NET30", "FOB", null, null, null, null,
                List.of(new PoLineInput("MAT-1", "Bracket", new BigDecimal("10"), "EA",
                        new BigDecimal("12.50"), "USD", null, null, null, "P01", null, null))
        );
        PurchaseOrderResponse draft = service.create(create);
        service.submit(draft.purchaseOrderId());
        service.decide(draft.purchaseOrderId(), new DecidePoRequest("APPROVE", "OK"));
        service.issue(draft.purchaseOrderId(), new IssuePoRequest("ERP-1"));

        PurchaseOrderResponse partial = service.close(
                draft.purchaseOrderId(), new ClosePoRequest("first lot", true));
        assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, partial.status());

        PurchaseOrderResponse closed = service.close(
                draft.purchaseOrderId(), new ClosePoRequest("complete", false));
        assertEquals(PurchaseOrderStatus.CLOSED, closed.status());
    }

}
