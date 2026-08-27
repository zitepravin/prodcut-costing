package com.automotive.costing.security;

import com.automotive.costing.platform.access.service.EffectivePermissionService;
import com.automotive.costing.platform.user.service.PermissionMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * SpEL: {@code @permissionAuth.has('CODE')}
 * Uses EffectivePermissionService when available (platform entitlement AND role);
 * falls back to PermissionMatrixService Role Matrix.
 */
@Component("permissionAuth")
@RequiredArgsConstructor
public class PermissionAuth {

    private final PermissionMatrixService permissionMatrixService;
    private final ObjectProvider<EffectivePermissionService> effectivePermissionService;

    public boolean has(String permissionCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        var roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        EffectivePermissionService eps = effectivePermissionService.getIfAvailable();
        if (eps != null) {
            return eps.hasEffectivePermission(roles, permissionCode);
        }
        return permissionMatrixService.hasPermission(roles, permissionCode);
    }
}
