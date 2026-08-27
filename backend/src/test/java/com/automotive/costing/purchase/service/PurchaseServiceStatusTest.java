package com.automotive.costing.purchase.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.service.CentralAccessService;
import com.automotive.costing.purchase.entity.PurchaseRequestLine;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.automotive.costing.vendorselection.entity.SupplierTcoCalculation;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import com.automotive.costing.vendorselection.repository.SupplierTcoCalculationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * P3-01 hardening tests (A/B/C/D/G policy + TCO snapshot). No Spring context.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PurchaseServiceStatusTest {

    @Mock SupplierQualificationCurrentRepository qualificationCurrentRepository;
    @Mock SupplierTcoCalculationRepository tcoCalculationRepository;
    @Mock CentralAccessService centralAccessService;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks PurchaseService purchaseService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
        CustomerContext.clear();
    }

    @Test
    void notAvailableConstantIsStable() {
        assertEquals("NOT_AVAILABLE", PurchaseService.NOT_AVAILABLE);
        assertEquals("AVAILABLE", PurchaseService.AVAILABLE);
        assertEquals("PURCHASE_REQUEST", PurchaseService.ENTITY_TYPE);
    }

    @Test
    void currentUserId_readsAuthenticatedPrincipal() {
        assertNull(PurchaseService.currentUserId());
        // Spring Security: 2-arg constructor is unauthenticated; use authorities form
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-abc", "n/a", java.util.List.of()));
        assertEquals("user-abc", PurchaseService.currentUserId());
    }

    @Test
    void assertSupplierApproved_failsWhenMissing() {
        CustomerContext.setCustomerId("cust-vs");
        // VS entitled → missing qualification fails
        // doNothing is default for void unless we throw — assertModuleEnabled succeeds
        when(qualificationCurrentRepository.findByCustomerIdAndSupplierId("cust-vs", "s1")).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> purchaseService.assertSupplierApprovedForPurchase("s1"));
        assertTrue(ex.getMessage().contains("APPROVED"));
    }

    @Test
    void assertSupplierApproved_failsWhenBlocked() {
        CustomerContext.setCustomerId("cust-test-1");
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("s1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.BLOCKED);
        when(qualificationCurrentRepository.findByCustomerIdAndSupplierId("cust-test-1", "s1")).thenReturn(Optional.of(cur));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> purchaseService.assertSupplierApprovedForPurchase("s1"));
        assertTrue(ex.getMessage().contains("BLOCKED"));
    }

    @Test
    void assertSupplierApproved_passesWhenApproved() {
        CustomerContext.setCustomerId("cust-vs");
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("s1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.APPROVED);
        when(qualificationCurrentRepository.findByCustomerIdAndSupplierId("cust-vs", "s1")).thenReturn(Optional.of(cur));
        assertDoesNotThrow(() -> purchaseService.assertSupplierApprovedForPurchase("s1"));
    }

    @Test
    void applyTcoSnapshot_setsAvailableWhenComplete() {
        SupplierTcoCalculation tco = new SupplierTcoCalculation();
        tco.setTcoId("t1");
        tco.setSupplierId("s1");
        tco.setBomNodeId("b1");
        tco.setDataComplete(true);
        tco.setTcoTotal(new BigDecimal("120.00"));
        tco.setAnnualVolume(new BigDecimal("10"));
        tco.setCalculatedAt(Instant.now());
        when(tcoCalculationRepository.findBySupplierIdOrderByCalculatedAtDesc("s1"))
                .thenReturn(List.of(tco));

        PurchaseRequestLine line = new PurchaseRequestLine();
        purchaseService.applyTcoSnapshot(line, "s1", "b1");
        assertEquals(PurchaseService.AVAILABLE, line.getTcoStatus());
        assertEquals(0, new BigDecimal("12.000000").compareTo(line.getTcoUnit()));
    }

    @Test
    void applyTcoSnapshot_staysNotAvailableWhenIncomplete() {
        SupplierTcoCalculation tco = new SupplierTcoCalculation();
        tco.setTcoId("t1");
        tco.setSupplierId("s1");
        tco.setBomNodeId("b1");
        tco.setDataComplete(false);
        tco.setTcoTotal(new BigDecimal("50"));
        tco.setCalculatedAt(Instant.now());
        when(tcoCalculationRepository.findBySupplierIdOrderByCalculatedAtDesc("s1"))
                .thenReturn(List.of(tco));

        PurchaseRequestLine line = new PurchaseRequestLine();
        purchaseService.applyTcoSnapshot(line, "s1", "b1");
        assertEquals(PurchaseService.NOT_AVAILABLE, line.getTcoStatus());
        assertNull(line.getTcoUnit());
    }

    @Test
    void applyTcoSnapshot_staysNotAvailableWhenNoMatch() {
        when(tcoCalculationRepository.findBySupplierIdOrderByCalculatedAtDesc("s1"))
                .thenReturn(List.of());
        PurchaseRequestLine line = new PurchaseRequestLine();
        purchaseService.applyTcoSnapshot(line, "s1", "b1");
        assertEquals(PurchaseService.NOT_AVAILABLE, line.getTcoStatus());
        assertNull(line.getTcoUnit());
    }
}
