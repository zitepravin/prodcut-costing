package com.automotive.costing.purchase.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.currency.service.CurrencyService;
import com.automotive.costing.makevsbuy.repository.MakeVsBuyLegRepository;
import com.automotive.costing.makevsbuy.repository.MakeVsBuySimulationRepository;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.service.CentralAccessService;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.purchase.dto.PurchaseDtos.CreateStandalonePurchaseRequest;
import com.automotive.costing.purchase.dto.PurchaseDtos.LineInput;
import com.automotive.costing.purchase.dto.PurchaseDtos.PurchaseRequestResponse;
import com.automotive.costing.purchase.entity.PurchaseRequest;
import com.automotive.costing.purchase.entity.PurchaseRequestLine;
import com.automotive.costing.purchase.repository.PurchaseRequestLineRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import com.automotive.costing.rfq.repository.SupplierMasterRepository;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.rfq.service.RfqService;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import com.automotive.costing.vendorselection.repository.SupplierTcoCalculationRepository;
import com.automotive.costing.workflow.service.ApprovalWorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseStandaloneCreateTest {

    @Mock PurchaseRequestRepository requestRepository;
    @Mock PurchaseRequestLineRepository lineRepository;
    @Mock BomNodeRepository bomNodeRepository;
    @Mock MakeVsBuySimulationRepository simulationRepository;
    @Mock MakeVsBuyLegRepository legRepository;
    @Mock SupplierQuoteRepository quoteRepository;
    @Mock SupplierMasterRepository supplierMasterRepository;
    @Mock SupplierQualificationCurrentRepository qualificationCurrentRepository;
    @Mock SupplierTcoCalculationRepository tcoCalculationRepository;
    @Mock AccessScopeService accessScopeService;
    @Mock CentralAccessService centralAccessService;
    @Mock CurrencyService currencyService;
    @Mock RfqService rfqService;
    @Mock ApprovalWorkflowService approvalWorkflowService;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks PurchaseService service;

    private final AtomicReference<PurchaseRequest> stored = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        CustomerContext.setCustomerId("purchase-only-tenant");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "n/a"));
        when(requestRepository.save(any(PurchaseRequest.class))).thenAnswer(inv -> {
            stored.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(lineRepository.save(any(PurchaseRequestLine.class))).thenAnswer(inv -> inv.getArgument(0));
        // VS not entitled
        doThrow(new RuntimeException("ACCESS-02")).when(centralAccessService)
                .assertModuleEnabled("purchase-only-tenant", "VENDOR_SELECTION");
    }

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
        SecurityContextHolder.clearContext();
        stored.set(null);
    }

    @Test
    void createStandalone_noBomNoProgram() {
        CreateStandalonePurchaseRequest req = new CreateStandalonePurchaseRequest(
                "P01", null, null, LocalDate.now().plusDays(14), "HIGH", "Buy brackets",
                "USD", null,
                List.of(new LineInput(null, null, "MAT-STANDALONE", "Bracket",
                        new BigDecimal("10"), "EA", new BigDecimal("5"), "USD",
                        null, null, null))
        );
        PurchaseRequestResponse res = service.createStandalone(req);
        assertNotNull(res.purchaseRequestId());
        assertEquals(PurchaseRequestStatus.DRAFT, res.status());
        assertNull(stored.get().getBomNodeId());
        assertNull(stored.get().getProgramId());
        assertEquals("purchase-only-tenant", stored.get().getCustomerId());
    }

    @Test
    void purchaseOnly_allowsSupplierWithoutQualification() {
        // no qualification row
        when(qualificationCurrentRepository.findById("sup-free")).thenReturn(java.util.Optional.empty());
        assertDoesNotThrow(() -> service.assertSupplierApprovedForPurchase("sup-free"));
    }
}
