package com.automotive.costing.platform.access;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.service.CentralAccessService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;

/**
 * Enforces module entitlement for methods or controller classes annotated with {@link RequiresModule}.
 * Order for mutating endpoints: Tenant (context) → License (if licensed) → Module.
 * Read-only (GET) endpoints skip license operational checks.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RequiresModuleAspect {

    private final CentralAccessService centralAccessService;

    @Before("@within(com.automotive.costing.platform.access.RequiresModule) || @annotation(com.automotive.costing.platform.access.RequiresModule)")
    public void checkModule(JoinPoint joinPoint) {
        RequiresModule requiresModule = resolveAnnotation(joinPoint);
        if (requiresModule == null) {
            return;
        }
        String customerId = CustomerContext.getCustomerId();
        if (customerId == null || customerId.isBlank()) {
            throw new BusinessRuleViolationException(
                    "X-Customer-Id header is required for module-scoped operations", "ACCESS-07");
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (isMutating(method)) {
            centralAccessService.assertLicenseActiveIfLicensed(customerId);
        }
        centralAccessService.assertModuleEnabled(customerId, requiresModule.value());
    }

    private static boolean isMutating(Method method) {
        return method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }

    private static RequiresModule resolveAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresModule onMethod = method.getAnnotation(RequiresModule.class);
        if (onMethod != null) {
            return onMethod;
        }
        Class<?> targetClass = joinPoint.getTarget() != null
                ? joinPoint.getTarget().getClass()
                : method.getDeclaringClass();
        RequiresModule onClass = targetClass.getAnnotation(RequiresModule.class);
        if (onClass != null) {
            return onClass;
        }
        Class<?> superClass = targetClass.getSuperclass();
        if (superClass != null) {
            return superClass.getAnnotation(RequiresModule.class);
        }
        return null;
    }
}
