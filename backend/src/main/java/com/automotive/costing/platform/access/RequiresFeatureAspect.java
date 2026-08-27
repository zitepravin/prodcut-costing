package com.automotive.costing.platform.access;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.service.CentralAccessService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class RequiresFeatureAspect {

    private final CentralAccessService centralAccessService;

    @Before("@within(com.automotive.costing.platform.access.RequiresFeature) || @annotation(com.automotive.costing.platform.access.RequiresFeature)")
    public void checkFeature(JoinPoint joinPoint) {
        RequiresFeature requiresFeature = resolveAnnotation(joinPoint);
        if (requiresFeature == null) {
            return;
        }
        String customerId = CustomerContext.getCustomerId();
        if (customerId == null || customerId.isBlank()) {
            throw new BusinessRuleViolationException(
                    "X-Customer-Id header is required for feature-scoped operations", "ACCESS-FEAT-01");
        }
        centralAccessService.assertFeatureEnabled(customerId, requiresFeature.value());
    }

    private static RequiresFeature resolveAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresFeature onMethod = method.getAnnotation(RequiresFeature.class);
        if (onMethod != null) {
            return onMethod;
        }
        Class<?> targetClass = joinPoint.getTarget().getClass();
        RequiresFeature onClass = targetClass.getAnnotation(RequiresFeature.class);
        if (onClass != null) {
            return onClass;
        }
        return AopUtils.getTargetClass(joinPoint.getTarget()).getAnnotation(RequiresFeature.class);
    }
}
