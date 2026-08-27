package com.automotive.costing.platform.access;

import com.automotive.costing.platform.user.service.PermissionMatrixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V224 — Customer Admin boundary tests.
 * Customer Admin manages own-tenant users only; cannot elevate to platform roles
 * or grant platform-only permissions.
 */
class CustomerAdminBoundaryTest {

    private PermissionMatrixService matrix;

    @BeforeEach
    void setUp() {
        matrix = new PermissionMatrixService();
    }

    @Test
    void customerAdmin_cannotAssignPlatformOwner() {
        assertFalse(matrix.isAssignableByCustomerAdmin("PLATFORM_OWNER"));
    }

    @Test
    void customerAdmin_cannotAssignSystemAdministrator() {
        assertFalse(matrix.isAssignableByCustomerAdmin("SYSTEM_ADMINISTRATOR"));
    }

    @Test
    void customerAdmin_cannotAssignAdminAlias() {
        assertFalse(matrix.isAssignableByCustomerAdmin("ADMIN"));
    }

    @Test
    void customerAdmin_canAssignOperationalRoles() {
        assertTrue(matrix.isAssignableByCustomerAdmin("COSTING_ENGINEER"));
        assertTrue(matrix.isAssignableByCustomerAdmin("PURCHASING_BUYER"));
        assertTrue(matrix.isAssignableByCustomerAdmin("COSTING_MANAGER"));
        assertTrue(matrix.isAssignableByCustomerAdmin("VIEWER"));
        assertTrue(matrix.isAssignableByCustomerAdmin("PRODUCT_COST_ENGINEER"));
        assertTrue(matrix.isAssignableByCustomerAdmin("COMMODITY_MATERIAL_BUYER"));
        assertTrue(matrix.isAssignableByCustomerAdmin("INDUSTRIAL_ENGINEER"));
        assertTrue(matrix.isAssignableByCustomerAdmin("PURCHASING_MANAGER"));
    }

    @Test
    void customerAdmin_cannotAssignApprovalOverride() {
        assertFalse(matrix.isAssignableByCustomerAdmin("APPROVAL_OVERRIDE"));
    }

    @Test
    void customerAdmin_roleDoesNotIncludePlatformAdminPermission() {
        assertFalse(matrix.permissionsForRole("CUSTOMER_ADMIN").contains("PLATFORM_ADMIN"));
        assertFalse(matrix.permissionsForRole("CUSTOMER_ADMIN").contains("TENANT_MANAGE"));
        assertFalse(matrix.permissionsForRole("CUSTOMER_ADMIN").contains("SUBSCRIPTION_MANAGE"));
    }

    @Test
    void forbiddenRoles_areDisjointFromAssignable() {
        for (String forbidden : PermissionMatrixService.CUSTOMER_ADMIN_FORBIDDEN_ROLES) {
            assertFalse(
                    PermissionMatrixService.CUSTOMER_ADMIN_ASSIGNABLE_ROLES.contains(forbidden),
                    "Forbidden role must not appear in assignable set: " + forbidden);
        }
    }
}
