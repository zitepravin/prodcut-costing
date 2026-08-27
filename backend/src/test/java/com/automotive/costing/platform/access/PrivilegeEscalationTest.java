package com.automotive.costing.platform.access;

import com.automotive.costing.platform.user.service.PermissionMatrixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V224 — Privilege escalation tests.
 * Ensures lower-privilege roles cannot obtain platform capabilities through the matrix,
 * and that platform-only permissions remain isolated.
 */
class PrivilegeEscalationTest {

    private PermissionMatrixService matrix;

    @BeforeEach
    void setUp() {
        matrix = new PermissionMatrixService();
    }

    @Test
    void costingEngineer_cannotEscalateToUserAdmin() {
        List<String> perms = matrix.permissionsForRole("COSTING_ENGINEER");
        assertFalse(perms.contains("USER_CREATE"));
        assertFalse(perms.contains("USER_DISABLE"));
        assertFalse(perms.contains("ROLE_MANAGE"));
    }

    @Test
    void buyer_cannotEscalateToCostingApproveOrPlatform() {
        List<String> perms = matrix.permissionsForRole("PURCHASING_BUYER");
        assertFalse(perms.contains("COSTING_APPROVE"));
        assertFalse(perms.contains("PLATFORM_ADMIN"));
        assertFalse(perms.contains("TENANT_MANAGE"));
    }

    @Test
    void viewer_hasNoMutatingPermissions() {
        List<String> perms = matrix.permissionsForRole("VIEWER");
        Set<String> mutating = Set.of(
                "BOM_CREATE", "BOM_EDIT", "BOM_DELETE",
                "COSTING_CREATE", "COSTING_EDIT", "COSTING_APPROVE",
                "RFQ_CREATE", "RFQ_EDIT", "RFQ_DELETE",
                "SUPPLIER_CREATE", "SUPPLIER_EDIT",
                "QUOTE_CREATE", "QUOTE_EDIT",
                "USER_CREATE", "USER_EDIT", "USER_DISABLE"
        );
        for (String m : mutating) {
            assertFalse(perms.contains(m), "VIEWER must not have " + m);
        }
    }

    @Test
    void nonPlatformRoles_excludeAllPlatformOnlyPermissions() {
        String[] operational = {
                "COSTING_ENGINEER", "COSTING_MANAGER", "PURCHASING_BUYER",
                "SOURCING_LEAD", "MASTER_DATA_ADMIN", "FINANCE_CONTROLLER",
                "PROGRAM_COST_MANAGER", "PROCESS_ENGINEER", "TOOLING_ENGINEER",
                "VIEWER", "CUSTOMER_ADMIN"
        };
        for (String role : operational) {
            List<String> perms = matrix.permissionsForRole(role);
            for (String p : PermissionMatrixService.PLATFORM_ONLY_PERMISSIONS) {
                assertFalse(perms.contains(p),
                        role + " must not have platform-only permission " + p);
            }
        }
    }

    @Test
    void systemAdministrator_isSupersetOfOperationalPermissions() {
        Set<String> admin = new HashSet<>(matrix.permissionsForRole("SYSTEM_ADMINISTRATOR"));
        for (String op : List.of("BOM_VIEW", "COSTING_VIEW", "RFQ_VIEW", "USER_VIEW")) {
            assertTrue(admin.contains(op), "SYSTEM_ADMINISTRATOR missing " + op);
        }
        assertTrue(admin.containsAll(PermissionMatrixService.PLATFORM_ONLY_PERMISSIONS));
    }

    @Test
    void financeController_cannotCreateUsersOrBom() {
        List<String> perms = matrix.permissionsForRole("FINANCE_CONTROLLER");
        assertTrue(perms.contains("COSTING_APPROVE"));
        assertFalse(perms.contains("USER_CREATE"));
        assertFalse(perms.contains("BOM_CREATE"));
        assertFalse(perms.contains("COSTING_CREATE"));
    }

    @Test
    void viewer_cannotApproveOrConfigure() {
        List<String> perms = matrix.permissionsForRole("VIEWER");
        assertFalse(perms.contains("COSTING_APPROVE"));
        assertFalse(perms.contains("PLATFORM_ADMIN"));
        assertTrue(perms.contains("COSTING_VIEW"));
    }

}
