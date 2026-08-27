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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequiresModuleAspectTest {

    @Mock
    private CentralAccessService centralAccessService;

    @InjectMocks
    private RequiresModuleAspect aspect;

    @AfterEach
    void clearContext() {
        CustomerContext.clear();
    }

    @RequiresModule("VENDOR_SELECTION")
    static class AnnotatedController {
        public void sample() {}
    }

    @Test
    void missingCustomerHeader_ACCESS_07() throws Exception {
        JoinPoint jp = joinPointFor(new AnnotatedController(), "sample");
        assertThatThrownBy(() -> aspect.checkModule(jp))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("X-Customer-Id");
        verifyNoInteractions(centralAccessService);
    }

    @Test
    void enabledModule_delegatesToService() throws Exception {
        CustomerContext.setCustomerId("cust-1");
        JoinPoint jp = joinPointFor(new AnnotatedController(), "sample");
        doNothing().when(centralAccessService).assertModuleEnabled("cust-1", "VENDOR_SELECTION");
        aspect.checkModule(jp);
        verify(centralAccessService).assertModuleEnabled("cust-1", "VENDOR_SELECTION");
    }

    @Test
    void disabledModule_propagatesACCESS_03() throws Exception {
        CustomerContext.setCustomerId("cust-a");
        JoinPoint jp = joinPointFor(new AnnotatedController(), "sample");
        doThrow(new BusinessRuleViolationException(
                "Module VENDOR_SELECTION is disabled for this customer", "ACCESS-03"))
                .when(centralAccessService).assertModuleEnabled("cust-a", "VENDOR_SELECTION");
        assertThatThrownBy(() -> aspect.checkModule(jp))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("disabled");
    }

    private static JoinPoint joinPointFor(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        MethodSignature sig = mock(MethodSignature.class);
        when(sig.getMethod()).thenReturn(method);
        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getSignature()).thenReturn(sig);
        when(jp.getTarget()).thenReturn(target);
        return jp;
    }
}
