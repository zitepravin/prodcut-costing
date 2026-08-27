package com.automotive.costing.platform.access.controller;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.service.EffectivePermissionService;
import com.automotive.costing.platform.access.service.EffectivePermissionService.RolePermissionRow;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer/role-permissions")
@RequiredArgsConstructor
@Tag(name = "Customer Role Permissions")
public class CustomerRolePermissionController {

    private final EffectivePermissionService effectivePermissionService;

    private String requireTenant() {
        String cid = CustomerContext.getCustomerId();
        if (cid == null || cid.isBlank()) {
            throw new BusinessRuleViolationException("Customer context required (X-Customer-Id)", "ACCESS-10");
        }
        return cid;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<List<RolePermissionRow>> list(@RequestParam String roleCode) {
        return ResponseEntity.ok(effectivePermissionService.listRolePermissions(requireTenant(), roleCode));
    }

    @PutMapping("/{permissionCode}")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<RolePermissionRow> set(
            @RequestParam String roleCode,
            @PathVariable String permissionCode,
            @RequestBody Map<String, Boolean> body) {
        boolean allowed = Boolean.TRUE.equals(body.get("allowed"));
        return ResponseEntity.ok(effectivePermissionService.setRolePermission(
                requireTenant(), roleCode, permissionCode, allowed));
    }
}
