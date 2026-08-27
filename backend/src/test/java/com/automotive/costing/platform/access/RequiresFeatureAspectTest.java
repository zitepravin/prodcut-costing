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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequiresFeatureAspectTest {

    @Mock CentralAccessService centralAccessService;
    @Mock JoinPoint joinPoint;
    @Mock MethodSignature methodSignature;
    @InjectMocks RequiresFeatureAspect aspect;

    @AfterEach
    void clear() { CustomerContext.clear(); }

    static class Sample {
        @RequiresFeature("SHOULD_COST_CALCULATE")
        public void calc() {}
    }

    @Test
    void missingCustomerId_denies() throws Exception {
        Method m = Sample.class.getMethod("calc");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(m);
        CustomerContext.clear();
        assertThatThrownBy(() -> aspect.checkFeature(joinPoint))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("X-Customer-Id");
        verifyNoInteractions(centralAccessService);
    }

    @Test
    void withCustomer_delegatesToService() throws Exception {
        Method m = Sample.class.getMethod("calc");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(m);
        CustomerContext.setCustomerId("cust-a");
        aspect.checkFeature(joinPoint);
        verify(centralAccessService).assertFeatureEnabled("cust-a", "SHOULD_COST_CALCULATE");
    }
}
