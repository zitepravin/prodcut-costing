package com.automotive.costing.governance;

import com.automotive.costing.platform.user.service.PermissionMatrixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V228 — Governance security: CUSTOMER_ADMIN is not a rate/costing approver;
 * platform-only permissions remain isolated.
 */
class CostingGovernanceSecurityTest {

    private PermissionMatrixService matrix;

    @BeforeEach
    void setUp() {
        matrix = new PermissionMatrixService();
    }

    @Test
    void customerAdmin_cannotBeAssignedPlatformOwnerOrSystemAdmin() {
        assertFalse(matrix.isAssignableByCustomerAdmin("PLATFORM_OWNER"));
        assertFalse(matrix.isAssignableByCustomerAdmin("SYSTEM_ADMINISTRATOR"));
        assertFalse(matrix.isAssignableByCustomerAdmin("ADMIN"));
    }

    @Test
    void customerAdmin_permissions_excludeCostingApproveAndPlatformAdmin() {
        List<String> perms = matrix.permissionsForRole("CUSTOMER_ADMIN");
        assertFalse(perms.contains("COSTING_APPROVE"));
        assertFalse(perms.contains("PLATFORM_ADMIN"));
        assertFalse(perms.contains("SUBSCRIPTION_MANAGE"));
        assertFalse(perms.contains("TENANT_MANAGE"));
        assertTrue(perms.contains("USER_CREATE"));
    }

    @Test
    void costingManager_hasCostingApprove_customerAdminDoesNot() {
        assertTrue(matrix.permissionsForRole("COSTING_MANAGER").contains("COSTING_APPROVE"));
        assertFalse(matrix.permissionsForRole("CUSTOMER_ADMIN").contains("COSTING_APPROVE"));
    }

    @Test
    void masterDataAdmin_notRateApproverByMatrixDefault() {
        // Rate master approve is COSTING_MANAGER at API layer; MASTER_DATA_ADMIN writes versions
        List<String> mda = matrix.permissionsForRole("MASTER_DATA_ADMIN");
        assertFalse(mda.contains("COSTING_APPROVE"));
    }

    @Test
    void platformOnlyPermissions_notGrantableViaCustomerAdminPath() {
        for (String p : PermissionMatrixService.PLATFORM_ONLY_PERMISSIONS) {
            assertTrue(matrix.isPlatformOnlyPermission(p));
            assertFalse(matrix.permissionsForRole("CUSTOMER_ADMIN").contains(p));
        }
    }
}
