package com.automotive.costing.purchase.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.CreatePurchaseOrderRequest;
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
 * Wave G — tenant isolation for Purchase Orders (service-level).
 * Module entitlement is enforced by @RequiresModule aspect (see PurchaseModuleGateTest).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseOrderSecurityTest {

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";

    @Mock PurchaseOrderRepository orderRepository;
    @Mock PurchaseOrderLineRepository lineRepository;
    @Mock PurchaseRequestRepository purchaseRequestRepository;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;

    @InjectMocks PurchaseOrderService service;

    private final AtomicReference<PurchaseOrder> stored = new AtomicReference<>();
    private final List<PurchaseOrderLine> lines = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-a", "n/a"));
        when(orderRepository.existsByCustomerIdAndPoNumber(anyString(), anyString())).thenReturn(false);
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            stored.set(po);
            return po;
        });
        when(orderRepository.findByPurchaseOrderIdAndCustomerId(anyString(), anyString()))
                .thenAnswer(inv -> {
                    String id = inv.getArgument(0);
                    String cid = inv.getArgument(1);
                    PurchaseOrder po = stored.get();
                    if (po == null || !po.getPurchaseOrderId().equals(id) || !po.getCustomerId().equals(cid)) {
                        return Optional.empty();
                    }
                    return Optional.of(po);
                });
        when(lineRepository.save(any(PurchaseOrderLine.class))).thenAnswer(inv -> {
            PurchaseOrderLine l = inv.getArgument(0);
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
    void createRequiresCustomerContext() {
        CustomerContext.clear();
        assertThrows(BusinessRuleViolationException.class, () -> service.create(sampleCreate()));
    }

    @Test
    void crossTenantCannotReadPo() {
        CustomerContext.setCustomerId(TENANT_A);
        PurchaseOrderResponse created = service.create(sampleCreate());
        assertEquals(TENANT_A, created.customerId());

        CustomerContext.setCustomerId(TENANT_B);
        assertThrows(ResourceNotFoundException.class, () -> service.get(created.purchaseOrderId()));
    }

    @Test
    void crossTenantCannotSubmitPo() {
        CustomerContext.setCustomerId(TENANT_A);
        PurchaseOrderResponse created = service.create(sampleCreate());

        CustomerContext.setCustomerId(TENANT_B);
        assertThrows(ResourceNotFoundException.class, () -> service.submit(created.purchaseOrderId()));
    }

    @Test
    void sameTenantCanReadOwnPo() {
        CustomerContext.setCustomerId(TENANT_A);
        PurchaseOrderResponse created = service.create(sampleCreate());
        PurchaseOrderResponse got = service.get(created.purchaseOrderId());
        assertEquals(created.purchaseOrderId(), got.purchaseOrderId());
        assertEquals(TENANT_A, got.customerId());
    }

    private static CreatePurchaseOrderRequest sampleCreate() {
        return new CreatePurchaseOrderRequest(
                null, null, "supplier-1", "P01", null, "USD",
                null, null, null, null, null, null,
                List.of(new PoLineInput("M1", null, BigDecimal.ONE, "EA",
                        BigDecimal.TEN, "USD", null, null, null, null, null, null))
        );
    }
}
