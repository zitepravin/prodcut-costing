package com.automotive.costing.platform.user.controller;

import com.automotive.costing.platform.user.dto.AdminDtos.*;
import com.automotive.costing.platform.user.dto.ConfidentialityWallDtos;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.platform.user.service.ConfidentialityWallService;
import com.automotive.costing.platform.user.service.PermissionMatrixService;
import com.automotive.costing.platform.user.service.RoleAdminService;
import com.automotive.costing.platform.user.service.UserAdminService;
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
 * M18 User Management, Roles &amp; Security - administration. See TDD Section 5.18 / SRS
 * Section 4.18. Every endpoint here is restricted to System Administrator - this is the
 * highest-privilege surface in the system (it can create accounts, grant roles, and
 * deactivate/reactivate anyone).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
@Tag(name = "Administration")
public class AdminController {

    private final UserAdminService userAdminService;
    private final RoleAdminService roleAdminService;
    private final AccessScopeService accessScopeService;
    private final ConfidentialityWallService confidentialityWallService;
    private final PermissionMatrixService permissionMatrixService;

    @PostMapping("/users")
    @Operation(summary = "Create a user (SEC-01)")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAdminService.createUser(request));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userAdminService.list());
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get a user by ID")
    public ResponseEntity<UserResponse> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userAdminService.getById(id));
    }

    @PutMapping("/users/{id}/roles")
    @Operation(summary = "Replace a user's role assignments (SEC-01)")
    public ResponseEntity<UserResponse> updateRoles(@PathVariable String id, @Valid @RequestBody UpdateRolesRequest request) {
        return ResponseEntity.ok(userAdminService.updateRoles(id, request));
    }

    @PutMapping("/users/{id}/deactivate")
    @Operation(summary = "Deactivate a user immediately, revoking all active sessions (SEC-08)")
    public ResponseEntity<UserResponse> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(userAdminService.deactivate(id));
    }

    @PutMapping("/users/{id}/reactivate")
    @Operation(summary = "Reactivate a previously deactivated user")
    public ResponseEntity<UserResponse> reactivate(@PathVariable String id) {
        return ResponseEntity.ok(userAdminService.reactivate(id));
    }

    @PostMapping("/roles")
    @Operation(summary = "Create a custom role (SEC-01)")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleAdminService.createCustomRole(request));
    }

    @GetMapping("/roles")
    @Operation(summary = "List all roles (system-seeded and custom)")
    public ResponseEntity<List<RoleResponse>> listRoles() {
        return ResponseEntity.ok(roleAdminService.list());
    }

    @PostMapping("/users/{id}/access-scopes")
    @Operation(summary = "Grant a user visibility into one plant/program/commodity value (SEC-03)")
    public ResponseEntity<ScopeResponse> grantScope(@PathVariable String id, @Valid @RequestBody GrantScopeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accessScopeService.grantScope(id, request));
    }

    @GetMapping("/users/{id}/access-scopes")
    @Operation(summary = "List a user's access scope grants (SEC-03)")
    public ResponseEntity<List<ScopeResponse>> listScopes(@PathVariable String id) {
        return ResponseEntity.ok(accessScopeService.listScopes(id));
    }

    @DeleteMapping("/access-scopes/{scopeId}")
    @Operation(summary = "Revoke an access scope grant (SEC-03)")
    public ResponseEntity<Void> revokeScope(@PathVariable String scopeId) {
        accessScopeService.revokeScope(scopeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/confidentiality-walls")
    @Operation(summary = "Configure an RFQ confidentiality wall (SEC-04)")
    public ResponseEntity<ConfidentialityWallDtos.Response> createConfidentialityWall(
            @Valid @RequestBody ConfidentialityWallDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(confidentialityWallService.create(request));
    }

    @GetMapping("/confidentiality-walls")
    @Operation(summary = "List confidentiality walls configured for an RFQ event (SEC-04)")
    public ResponseEntity<List<ConfidentialityWallDtos.Response>> confidentialityWallsForRfqEvent(
            @RequestParam String rfqEventReference) {
        return ResponseEntity.ok(confidentialityWallService.byRfqEvent(rfqEventReference));
    }

    @GetMapping("/permission-matrix")
    @Operation(summary = "Full role → permission matrix (catalogue + grants; V224)")
    public ResponseEntity<PermissionMatrixService.PermissionMatrixResponse> permissionMatrix() {
        return ResponseEntity.ok(permissionMatrixService.fullMatrix());
    }
}
