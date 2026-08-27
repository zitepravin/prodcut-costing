package com.automotive.costing.platform.access;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.service.CentralAccessService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Wave G — PURCHASE module gate (backend aspect).
 * Frontend hiding is not security; this is the authoritative boundary.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseModuleGateTest {

    @Mock CentralAccessService centralAccessService;
    @InjectMocks RequiresModuleAspect aspect;

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    @RequiresModule("PURCHASE")
    static class PurchaseOrderApi {
        public void create() {}
        public void list() {}
    }

    @RequiresModule("PRODUCT_COSTING")
    static class CostingApi {
        public void bom() {}
    }

    @RequiresModule("VENDOR_SELECTION")
    static class VendorApi {
        public void score() {}
    }

    @Test
    void purchaseApi_missingCustomer_ACCESS_07() throws Exception {
        JoinPoint jp = joinPoint(new PurchaseOrderApi(), "create");
        assertThatThrownBy(() -> aspect.checkModule(jp))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("X-Customer-Id");
        verifyNoInteractions(centralAccessService);
    }

    @Test
    void purchaseApi_moduleDisabled_ACCESS_03() throws Exception {
        CustomerContext.setCustomerId("cust-costing-only");
        JoinPoint jp = joinPoint(new PurchaseOrderApi(), "create");
        doThrow(new BusinessRuleViolationException(
                "Module PURCHASE is disabled for this customer", "ACCESS-03"))
                .when(centralAccessService).assertModuleEnabled("cust-costing-only", "PURCHASE");

        assertThatThrownBy(() -> aspect.checkModule(jp))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("PURCHASE");
        verify(centralAccessService).assertModuleEnabled("cust-costing-only", "PURCHASE");
    }

    @Test
    void purchaseApi_moduleEnabled_ok() throws Exception {
        CustomerContext.setCustomerId("cust-purchase");
        JoinPoint jp = joinPoint(new PurchaseOrderApi(), "list");
        doNothing().when(centralAccessService).assertModuleEnabled("cust-purchase", "PURCHASE");
        assertDoesNotThrow(() -> aspect.checkModule(jp));
        verify(centralAccessService).assertModuleEnabled("cust-purchase", "PURCHASE");
    }

    @Test
    void productCosting_notEntitled_blocked() throws Exception {
        CustomerContext.setCustomerId("cust-purchase-only");
        JoinPoint jp = joinPoint(new CostingApi(), "bom");
        doThrow(new BusinessRuleViolationException(
                "Module PRODUCT_COSTING is not entitled for this customer", "ACCESS-02"))
                .when(centralAccessService).assertModuleEnabled("cust-purchase-only", "PRODUCT_COSTING");

        assertThatThrownBy(() -> aspect.checkModule(jp))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("PRODUCT_COSTING");
    }

    @Test
    void vendorSelection_notEntitled_blocked() throws Exception {
        CustomerContext.setCustomerId("cust-purchase-only");
        JoinPoint jp = joinPoint(new VendorApi(), "score");
        doThrow(new BusinessRuleViolationException(
                "Module VENDOR_SELECTION is not entitled for this customer", "ACCESS-02"))
                .when(centralAccessService).assertModuleEnabled("cust-purchase-only", "VENDOR_SELECTION");

        assertThatThrownBy(() -> aspect.checkModule(jp))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("VENDOR_SELECTION");
    }

    private static JoinPoint joinPoint(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        MethodSignature sig = mock(MethodSignature.class);
        when(sig.getMethod()).thenReturn(method);
        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getSignature()).thenReturn(sig);
        when(jp.getTarget()).thenReturn(target);
        return jp;
    }
}
