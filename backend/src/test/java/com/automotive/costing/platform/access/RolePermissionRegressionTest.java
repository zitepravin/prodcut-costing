package com.automotive.costing.platform.access;

import com.automotive.costing.platform.user.service.PermissionMatrixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V224 — Role/permission regression: critical business mappings must not silently change.
 */
class RolePermissionRegressionTest {

    private PermissionMatrixService matrix;

    @BeforeEach
    void setUp() {
        matrix = new PermissionMatrixService();
    }

    @Test
    void managementStyle_costingManager_hasApproveAndReports() {
        List<String> p = matrix.permissionsForRole("COSTING_MANAGER");
        assertTrue(p.contains("COSTING_APPROVE"));
        assertTrue(p.contains("REPORT_VIEW"));
        assertTrue(p.contains("DASHBOARD_VIEW"));
    }

    @Test
    void sourcingLead_hasRfqDeleteAndSupplierEdit() {
        List<String> p = matrix.permissionsForRole("SOURCING_LEAD");
        assertTrue(p.contains("RFQ_DELETE"));
        assertTrue(p.contains("SUPPLIER_EDIT"));
        assertTrue(p.contains("QUOTE_EDIT"));
    }

    @Test
    void financeController_hasApproveNotUserAdmin() {
        List<String> p = matrix.permissionsForRole("FINANCE_CONTROLLER");
        assertTrue(p.contains("COSTING_APPROVE"));
        assertFalse(p.contains("USER_CREATE"));
    }

    @Test
    void catalogue_groupsAreNonEmpty() {
        var cat = matrix.fullMatrix().catalogue();
        assertTrue(cat.stream().anyMatch(d -> "Product Costing".equals(d.group())));
        assertTrue(cat.stream().anyMatch(d -> "Sourcing".equals(d.group())));
        assertTrue(cat.stream().anyMatch(d -> "Administration".equals(d.group())));
        assertTrue(cat.stream().anyMatch(d -> "Platform".equals(d.group())));
    }

    @Test
    void everyRoleInMatrix_hasAtLeastOnePermission() {
        for (var role : matrix.fullMatrix().roles()) {
            assertFalse(role.grantedPermissions().isEmpty(),
                    role.roleCode() + " must grant at least one permission");
        }
    }

    @Test
    void deniedHighlights_forCustomerAdminIncludePlatformAdmin() {
        var view = matrix.fullMatrix().roles().stream()
                .filter(r -> "CUSTOMER_ADMIN".equals(r.roleCode()))
                .findFirst()
                .orElseThrow();
        assertTrue(view.deniedHighlights().contains("PLATFORM_ADMIN")
                || !view.grantedPermissions().contains("PLATFORM_ADMIN"));
    }
}
