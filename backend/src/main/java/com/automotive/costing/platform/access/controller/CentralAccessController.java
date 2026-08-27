package com.automotive.costing.platform.access.controller;

import com.automotive.costing.platform.access.dto.AccessDtos.AddMembershipRequest;
import com.automotive.costing.platform.access.dto.AccessDtos.CreateCustomerRequest;
import com.automotive.costing.platform.access.dto.AccessDtos.UpdateCustomerRequest;
import com.automotive.costing.platform.access.dto.AccessDtos.SetCustomerStatusRequest;
import com.automotive.costing.platform.access.dto.AccessDtos.CustomerResponse;
import com.automotive.costing.platform.access.dto.AccessDtos.EffectiveAccessResponse;
import com.automotive.costing.platform.access.dto.AccessDtos.EntitlementResponse;
import com.automotive.costing.platform.access.dto.AccessDtos.MembershipResponse;
import com.automotive.costing.platform.access.dto.AccessDtos.ModuleResponse;
import com.automotive.costing.platform.access.dto.AccessDtos.SetEntitlementRequest;
import com.automotive.costing.platform.access.service.CentralAccessService;
import com.automotive.costing.platform.access.service.SupportAccessService;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.service.PermissionMatrixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Central Access Management APIs — single authority for tenant/module entitlement.
 * Reuses JWT + RBAC; does not introduce a second auth system.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Central Access Management")
public class CentralAccessController {

    private final CentralAccessService centralAccessService;
    private final SupportAccessService supportAccessService;
    private final PermissionMatrixService permissionMatrixService;

    @GetMapping("/me/access")
    @Operation(summary = "Effective access for the current user (customers, enabled modules, roles)")
    public ResponseEntity<EffectiveAccessResponse> myAccess(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(centralAccessService.effectiveAccess(user));
    }

    @GetMapping("/platform/modules")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "List registered platform modules")
    public ResponseEntity<List<ModuleResponse>> listModules() {
        return ResponseEntity.ok(centralAccessService.listModules());
    }

    @GetMapping("/platform/customers")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "List customers/tenants")
    public ResponseEntity<List<CustomerResponse>> listCustomers() {
        return ResponseEntity.ok(centralAccessService.listCustomers());
    }

    @GetMapping("/platform/customers/{customerId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Get customer/tenant by id")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(centralAccessService.getCustomer(customerId));
    }

    @GetMapping("/platform/customers/{customerId}/modules")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "List module entitlements for a customer")
    public ResponseEntity<List<EntitlementResponse>> listCustomerModules(@PathVariable String customerId) {
        return ResponseEntity.ok(centralAccessService.listEntitlements(customerId));
    }

    @GetMapping("/platform/customers/{customerId}/users")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "List user memberships for a customer")
    public ResponseEntity<List<MembershipResponse>> listCustomerUsers(@PathVariable String customerId) {
        return ResponseEntity.ok(centralAccessService.listMemberships(customerId));
    }

    @PostMapping("/platform/customers")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Create customer/tenant")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(centralAccessService.createCustomer(request));
    }

    @PostMapping("/platform/entitlements")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Enable or disable a module for a customer")
    public ResponseEntity<EntitlementResponse> setEntitlement(@Valid @RequestBody SetEntitlementRequest request) {
        return ResponseEntity.ok(centralAccessService.setEntitlement(request));
    }

    @PostMapping("/platform/memberships")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Add user membership to a customer")
    public ResponseEntity<MembershipResponse> addMembership(@Valid @RequestBody AddMembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(centralAccessService.addMembership(request));
    }

    @PutMapping("/platform/customers/{customerId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Update customer name")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(centralAccessService.updateCustomer(customerId, request));
    }

    @PutMapping("/platform/customers/{customerId}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Activate, suspend, or deactivate a customer")
    public ResponseEntity<CustomerResponse> setCustomerStatus(
            @PathVariable String customerId,
            @Valid @RequestBody SetCustomerStatusRequest request) {
        return ResponseEntity.ok(centralAccessService.setCustomerStatus(customerId, request));
    }

    @PutMapping("/platform/memberships/{membershipId}/deactivate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Deactivate user membership for a customer")
    public ResponseEntity<MembershipResponse> deactivateMembership(@PathVariable String membershipId) {
        return ResponseEntity.ok(centralAccessService.deactivateMembership(membershipId));
    }

    @GetMapping("/platform/dashboard-summary")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'PLATFORM_OWNER')")
    @Operation(summary = "Platform operational KPIs — no customer costing data")
    public ResponseEntity<CentralAccessService.PlatformDashboardSummary> dashboardSummary() {
        return ResponseEntity.ok(centralAccessService.platformDashboardSummary());
    }

    @PostMapping("/platform/support-access/request")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'PLATFORM_OWNER')")
    @Operation(summary = "Request time-limited support access to a customer tenant")
    public ResponseEntity<SupportAccessService.SupportSessionResponse> requestSupport(
            @RequestBody SupportAccessService.RequestSupportAccess body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportAccessService.request(body));
    }

    @PostMapping("/platform/support-access/{sessionId}/approve")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'CUSTOMER_ADMIN', 'COSTING_MANAGER')")
    @Operation(summary = "Customer admin approves support access")
    public ResponseEntity<SupportAccessService.SupportSessionResponse> approveSupport(@PathVariable String sessionId) {
        return ResponseEntity.ok(supportAccessService.approve(sessionId));
    }

    @PostMapping("/platform/support-access/{sessionId}/deny")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'CUSTOMER_ADMIN', 'COSTING_MANAGER')")
    public ResponseEntity<SupportAccessService.SupportSessionResponse> denySupport(@PathVariable String sessionId) {
        return ResponseEntity.ok(supportAccessService.deny(sessionId));
    }

    @PostMapping("/platform/support-access/{sessionId}/revoke")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'CUSTOMER_ADMIN', 'PLATFORM_OWNER')")
    public ResponseEntity<SupportAccessService.SupportSessionResponse> revokeSupport(@PathVariable String sessionId) {
        return ResponseEntity.ok(supportAccessService.revoke(sessionId));
    }

    @GetMapping("/platform/customers/{customerId}/support-access")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'PLATFORM_OWNER', 'CUSTOMER_ADMIN')")
    public ResponseEntity<java.util.List<SupportAccessService.SupportSessionResponse>> listSupport(
            @PathVariable String customerId) {
        return ResponseEntity.ok(supportAccessService.listForCustomer(customerId));
    }

    /**
     * Role permission preview for Customer Admin / managers (own-tenant guidance).
     * Does not expose platform-only role assignment capability.
     */
    @GetMapping("/customer/roles/{roleCode}/permissions")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'CUSTOMER_ADMIN', 'COSTING_MANAGER')")
    @Operation(summary = "Permissions granted by a role (display matrix; V224)")
    public ResponseEntity<List<String>> rolePermissions(@PathVariable String roleCode) {
        return ResponseEntity.ok(permissionMatrixService.permissionsForRole(roleCode));
    }

    @GetMapping("/customer/permission-matrix")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'CUSTOMER_ADMIN')")
    @Operation(summary = "Full permission matrix for Customer Admin UI (V224)")
    public ResponseEntity<PermissionMatrixService.PermissionMatrixResponse> customerPermissionMatrix() {
        return ResponseEntity.ok(permissionMatrixService.fullMatrix());
    }

    @GetMapping("/customer/roles/assignable")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'CUSTOMER_ADMIN')")
    @Operation(summary = "Roles Customer Admin may assign within their tenant")
    public ResponseEntity<List<String>> assignableRoles(
            @RequestParam(required = false, defaultValue = "false") boolean includePlatform) {
        if (includePlatform) {
            // Only platform operators should request includePlatform=true; still returns catalogue codes.
            return ResponseEntity.ok(List.copyOf(PermissionMatrixService.CUSTOMER_ADMIN_ASSIGNABLE_ROLES));
        }
        return ResponseEntity.ok(List.copyOf(PermissionMatrixService.CUSTOMER_ADMIN_ASSIGNABLE_ROLES));
    }
}
