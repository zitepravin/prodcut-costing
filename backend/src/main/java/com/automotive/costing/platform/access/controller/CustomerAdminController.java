package com.automotive.costing.platform.access.controller;

import com.automotive.costing.platform.access.dto.CustomerAdminDtos.*;
import com.automotive.costing.platform.access.service.CustomerDepartmentService;
import com.automotive.costing.platform.access.service.CustomerUserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * V225 — Customer Admin: tenant-scoped users and departments.
 * Security: CUSTOMER_ADMIN (+ platform admins). Tenant from CustomerContext only.
 */
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Customer Administration")
public class CustomerAdminController {

    private final CustomerUserAdminService userAdminService;
    private final CustomerDepartmentService departmentService;

    // ---- Users ----

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "List users in the current customer tenant")
    public ResponseEntity<List<CustomerUserResponse>> listUsers() {
        return ResponseEntity.ok(userAdminService.listUsers());
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Create user + invitation in current tenant")
    public ResponseEntity<CreateUserResult> createUser(@Valid @RequestBody CreateCustomerUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAdminService.createUser(request));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<CustomerUserResponse> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(userAdminService.getUser(userId));
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<CustomerUserResponse> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateCustomerUserRequest request) {
        return ResponseEntity.ok(userAdminService.updateUser(userId, request));
    }

    @PostMapping("/users/{userId}/activate")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<CustomerUserResponse> activate(@PathVariable String userId) {
        return ResponseEntity.ok(userAdminService.activate(userId));
    }

    @PostMapping("/users/{userId}/deactivate")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<CustomerUserResponse> deactivate(@PathVariable String userId) {
        return ResponseEntity.ok(userAdminService.deactivate(userId));
    }

    // ---- Invitation accept (authenticated not required — public token) ----

    @PostMapping("/invitations/accept")
    @Operation(summary = "Accept invitation with one-time token and set password")
    public ResponseEntity<InvitationAcceptResponse> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request) {
        return ResponseEntity.ok(userAdminService.acceptInvitation(request));
    }

    // ---- Departments ----

    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<List<DepartmentResponse>> listDepartments() {
        return ResponseEntity.ok(departmentService.list());
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(request));
    }

    @GetMapping("/departments/{departmentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<DepartmentResponse> getDepartment(@PathVariable String departmentId) {
        return ResponseEntity.ok(departmentService.get(departmentId));
    }

    @PutMapping("/departments/{departmentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable String departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.update(departmentId, request));
    }

    @PostMapping("/departments/{departmentId}/deactivate")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<DepartmentResponse> deactivateDepartment(@PathVariable String departmentId) {
        return ResponseEntity.ok(departmentService.deactivate(departmentId));
    }
}
