package com.automotive.costing.platform.user;

import com.automotive.costing.platform.user.service.PermissionMatrixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V224 — Full permission matrix regression.
 * Authorization remains role-based; matrix is the explicit catalogue for UI + boundary tests.
 */
class PermissionMatrixServiceTest {

    private PermissionMatrixService service;

    @BeforeEach
    void setUp() {
        service = new PermissionMatrixService();
    }

    @Test
    void fullMatrix_containsCatalogueAndRoles() {
        var matrix = service.fullMatrix();
        assertFalse(matrix.catalogue().isEmpty());
        assertFalse(matrix.roles().isEmpty());
        assertTrue(matrix.catalogue().stream().anyMatch(p -> "BOM_VIEW".equals(p.code())));
        assertTrue(matrix.roles().stream().anyMatch(r -> "COSTING_ENGINEER".equals(r.roleCode())));
    }

    @Test
    void buyer_hasRfqAndSupplier_notUserManage() {
        List<String> perms = service.permissionsForRole("PURCHASING_BUYER");
        assertTrue(perms.contains("RFQ_VIEW"));
        assertTrue(perms.contains("RFQ_CREATE"));
        assertTrue(perms.contains("SUPPLIER_VIEW"));
        assertTrue(perms.contains("QUOTE_VIEW"));
        assertFalse(perms.contains("USER_CREATE"));
        assertFalse(perms.contains("PLATFORM_ADMIN"));
        assertFalse(perms.contains("SUBSCRIPTION_MANAGE"));
    }

    @Test
    void costingEngineer_hasShouldCost_notPlatform() {
        List<String> perms = service.permissionsForRole("COSTING_ENGINEER");
        assertTrue(perms.contains("COSTING_CREATE"));
        assertTrue(perms.contains("SHOULD_COST"));
        assertTrue(perms.contains("MAKE_VS_BUY"));
        assertFalse(perms.contains("PLATFORM_ADMIN"));
        assertFalse(perms.contains("USER_DISABLE"));
    }

    @Test
    void customerAdmin_hasUserManage_notPlatformOwnerCapabilities() {
        List<String> perms = service.permissionsForRole("CUSTOMER_ADMIN");
        assertTrue(perms.contains("USER_VIEW"));
        assertTrue(perms.contains("USER_CREATE"));
        assertTrue(perms.contains("USER_DISABLE"));
        assertFalse(perms.contains("PLATFORM_ADMIN"));
        assertFalse(perms.contains("TENANT_MANAGE"));
        assertFalse(perms.contains("SUBSCRIPTION_MANAGE"));
        assertFalse(perms.contains("USER_MANAGE_ALL_TENANTS"));
    }

    @Test
    void platformOwner_hasTenantManage_notBomMutationsByDefault() {
        List<String> perms = service.permissionsForRole("PLATFORM_OWNER");
        assertTrue(perms.contains("TENANT_MANAGE"));
        assertTrue(perms.contains("SUBSCRIPTION_MANAGE"));
        assertFalse(perms.contains("BOM_CREATE"));
        assertFalse(perms.contains("COSTING_EDIT"));
    }

    @Test
    void customerAdmin_cannotAssignPlatformRoles() {
        assertFalse(service.isAssignableByCustomerAdmin("PLATFORM_OWNER"));
        assertFalse(service.isAssignableByCustomerAdmin("SYSTEM_ADMINISTRATOR"));
        assertFalse(service.isAssignableByCustomerAdmin("ADMIN"));
        assertTrue(service.isAssignableByCustomerAdmin("COSTING_ENGINEER"));
        assertTrue(service.isAssignableByCustomerAdmin("PURCHASING_BUYER"));
        assertTrue(service.isAssignableByCustomerAdmin("CUSTOMER_ADMIN"));
    }

    @Test
    void platformOnlyPermissions_areFlagged() {
        assertTrue(service.isPlatformOnlyPermission("PLATFORM_ADMIN"));
        assertTrue(service.isPlatformOnlyPermission("TENANT_MANAGE"));
        assertTrue(service.isPlatformOnlyPermission("SUBSCRIPTION_MANAGE"));
        assertFalse(service.isPlatformOnlyPermission("BOM_VIEW"));
        assertFalse(service.isPlatformOnlyPermission("USER_CREATE"));
    }

    @Test
    void viewer_isReadOnly() {
        List<String> perms = service.permissionsForRole("VIEWER");
        assertTrue(perms.contains("BOM_VIEW"));
        assertTrue(perms.contains("COSTING_VIEW"));
        assertFalse(perms.contains("BOM_CREATE"));
        assertFalse(perms.contains("COSTING_EDIT"));
        assertFalse(perms.contains("USER_CREATE"));
    }
}
