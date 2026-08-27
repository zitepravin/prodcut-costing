package com.automotive.costing.platform.access.controller;

import com.automotive.costing.platform.access.service.EffectivePermissionService;
import com.automotive.costing.platform.access.service.EffectivePermissionService.CatalogRow;
import com.automotive.costing.platform.access.service.EffectivePermissionService.EntitlementRow;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
@Tag(name = "Platform Entitlements")
public class PlatformEntitlementController {

    private final EffectivePermissionService effectivePermissionService;

    @GetMapping("/permission-catalog")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER','CUSTOMER_ADMIN')")
    public ResponseEntity<List<CatalogRow>> catalog() {
        return ResponseEntity.ok(effectivePermissionService.listCatalog());
    }

    @GetMapping("/customers/{customerId}/permission-entitlements")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    public ResponseEntity<List<EntitlementRow>> list(@PathVariable String customerId) {
        return ResponseEntity.ok(effectivePermissionService.listPlatformEntitlements(customerId));
    }

    @PutMapping("/customers/{customerId}/permission-entitlements/{permissionCode}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<EntitlementRow> set(
            @PathVariable String customerId,
            @PathVariable String permissionCode,
            @RequestBody Map<String, Boolean> body) {
        boolean allowed = Boolean.TRUE.equals(body.get("allowed"));
        return ResponseEntity.ok(
                effectivePermissionService.setPlatformEntitlement(customerId, permissionCode, allowed));
    }
}
