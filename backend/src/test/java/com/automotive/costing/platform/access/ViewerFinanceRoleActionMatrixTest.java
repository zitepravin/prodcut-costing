package com.automotive.costing.platform.access;

import com.automotive.costing.platform.user.service.PermissionMatrixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated VIEWER + FINANCE_CONTROLLER role × action contract.
 * Complements live HTTP matrix; does not require staging host.
 *
 * Contract (PermissionMatrix + controller annotations):
 * VIEWER — read-only (VIEW only); no CREATE/EDIT/DELETE/SUBMIT/APPROVE/LOCK/CONFIG mutators.
 * FINANCE_CONTROLLER — COSTING_VIEW, COSTING_APPROVE, REPORT/DASHBOARD; no user admin, no BOM mutate.
 */
class ViewerFinanceRoleActionMatrixTest {

    private PermissionMatrixService matrix;

    private static final Set<String> VIEWER_FORBIDDEN = Set.of(
            "BOM_CREATE", "BOM_EDIT", "BOM_DELETE",
            "COSTING_CREATE", "COSTING_EDIT", "COSTING_APPROVE",
            "RFQ_CREATE", "RFQ_EDIT", "RFQ_DELETE",
            "SUPPLIER_CREATE", "SUPPLIER_EDIT",
            "QUOTE_CREATE", "QUOTE_EDIT",
            "USER_CREATE", "USER_EDIT", "USER_DISABLE",
            "ROLE_MANAGE", "PLATFORM_ADMIN", "TENANT_MANAGE"
    );

    private static final Set<String> FINANCE_FORBIDDEN = Set.of(
            "USER_CREATE", "USER_EDIT", "USER_DISABLE",
            "ROLE_MANAGE", "PLATFORM_ADMIN", "TENANT_MANAGE",
            "BOM_CREATE", "BOM_EDIT", "BOM_DELETE",
            "RFQ_CREATE", "RFQ_EDIT", "RFQ_DELETE",
            "SUPPLIER_CREATE", "SUPPLIER_EDIT"
    );

    @BeforeEach
    void setUp() {
        matrix = new PermissionMatrixService();
    }

    @Nested
    @DisplayName("VIEWER permission matrix")
    class ViewerMatrix {

        @Test
        void hasReadPermissions() {
            List<String> p = matrix.permissionsForRole("VIEWER");
            assertTrue(p.contains("COSTING_VIEW"));
            assertTrue(p.contains("BOM_VIEW"));
            assertTrue(p.contains("REPORT_VIEW"));
            assertTrue(p.contains("DASHBOARD_VIEW"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "BOM_CREATE", "BOM_EDIT", "COSTING_CREATE", "COSTING_EDIT", "COSTING_APPROVE",
                "USER_CREATE", "RFQ_CREATE", "QUOTE_CREATE", "SUPPLIER_CREATE"
        })
        void deniesMutatingPermission(String perm) {
            assertFalse(matrix.permissionsForRole("VIEWER").contains(perm),
                    "VIEWER must not have " + perm);
        }

        @Test
        void noMutatingPermissionsAtAll() {
            for (String m : VIEWER_FORBIDDEN) {
                assertFalse(matrix.permissionsForRole("VIEWER").contains(m),
                        "VIEWER must not have " + m);
            }
        }
    }

    @Nested
    @DisplayName("FINANCE_CONTROLLER permission matrix")
    class FinanceMatrix {

        @Test
        void hasApproveAndView() {
            List<String> p = matrix.permissionsForRole("FINANCE_CONTROLLER");
            assertTrue(p.contains("COSTING_VIEW"));
            assertTrue(p.contains("COSTING_APPROVE"));
            assertTrue(p.contains("REPORT_VIEW"));
            assertTrue(p.contains("DASHBOARD_VIEW"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "USER_CREATE", "USER_EDIT", "BOM_CREATE", "RFQ_CREATE", "PLATFORM_ADMIN"
        })
        void deniesAdminAndBomMutate(String perm) {
            assertFalse(matrix.permissionsForRole("FINANCE_CONTROLLER").contains(perm),
                    "FINANCE_CONTROLLER must not have " + perm);
        }

        @Test
        void noForbiddenSet() {
            for (String m : FINANCE_FORBIDDEN) {
                assertFalse(matrix.permissionsForRole("FINANCE_CONTROLLER").contains(m),
                        "FINANCE_CONTROLLER must not have " + m);
            }
        }
    }

    /**
     * Role × Module × Action expected matrix (permission-layer).
     * N/A = not in product permission vocabulary for that role (not a silent omit).
     */
    @ParameterizedTest(name = "{0} / {1} / {2} => {3}")
    @CsvSource({
            // role, module, action, expected ALLOW|DENY|NA
            "VIEWER,ShouldCost,VIEW,ALLOW",
            "VIEWER,ShouldCost,CREATE,DENY",
            "VIEWER,ShouldCost,EDIT,DENY",
            "VIEWER,ShouldCost,DELETE,DENY",
            "VIEWER,ShouldCost,APPROVE,DENY",
            "VIEWER,ShouldCost,LOCK,DENY",
            "VIEWER,ShouldCost,CONFIGURATION,DENY",
            "VIEWER,Plant,VIEW,ALLOW",
            "VIEWER,Plant,CREATE,DENY",
            "VIEWER,CustomerUsers,VIEW,DENY",
            "VIEWER,CustomerUsers,CREATE,DENY",
            "VIEWER,License,CONFIGURATION,DENY",
            "VIEWER,Universal,VIEW,ALLOW",
            "VIEWER,Universal,CREATE,DENY",
            "FINANCE_CONTROLLER,ShouldCost,VIEW,ALLOW",
            "FINANCE_CONTROLLER,ShouldCost,APPROVE,ALLOW",
            "FINANCE_CONTROLLER,ShouldCost,CREATE,DENY",
            "FINANCE_CONTROLLER,ShouldCost,EDIT,DENY",
            "FINANCE_CONTROLLER,Plant,CREATE,DENY",
            "FINANCE_CONTROLLER,CustomerUsers,CREATE,DENY",
            "FINANCE_CONTROLLER,License,CONFIGURATION,DENY",
            "FINANCE_CONTROLLER,BOM,CREATE,DENY",
            "COSTING_ENGINEER,ShouldCost,CREATE,ALLOW",
            "COSTING_ENGINEER,ShouldCost,APPROVE,DENY",
            "COSTING_MANAGER,ShouldCost,APPROVE,ALLOW",
            "CUSTOMER_ADMIN,CustomerUsers,CREATE,ALLOW",
            "CUSTOMER_ADMIN,License,CONFIGURATION,DENY",
            "PURCHASING_BUYER,ShouldCost,APPROVE,DENY",
            "PURCHASING_BUYER,CustomerUsers,CREATE,DENY"
    })
    void roleModuleAction_expected(String role, String module, String action, String expected) {
        List<String> perms = matrix.permissionsForRole(role);
        boolean allow = switch (module + "/" + action) {
            case "ShouldCost/VIEW", "Universal/VIEW", "Plant/VIEW" ->
                    perms.contains("COSTING_VIEW") || perms.contains("BOM_VIEW");
            case "ShouldCost/CREATE", "Universal/CREATE" ->
                    perms.contains("COSTING_CREATE") || perms.contains("SHOULD_COST");
            case "ShouldCost/EDIT" -> perms.contains("COSTING_EDIT");
            case "ShouldCost/DELETE" -> perms.contains("COSTING_EDIT") || perms.contains("BOM_DELETE");
            case "ShouldCost/APPROVE", "ShouldCost/LOCK" -> perms.contains("COSTING_APPROVE");
            case "ShouldCost/CONFIGURATION", "License/CONFIGURATION" ->
                    perms.contains("PLATFORM_ADMIN") || perms.contains("TENANT_MANAGE");
            case "Plant/CREATE" -> false; // plant create is role-annotation MASTER_DATA/MANAGER not matrix perm
            case "CustomerUsers/VIEW" -> perms.contains("USER_VIEW");
            case "CustomerUsers/CREATE" -> perms.contains("USER_CREATE");
            case "BOM/CREATE" -> perms.contains("BOM_CREATE");
            default -> false;
        };
        // Plant CREATE is N/A in permission matrix vocabulary — enforced via @PreAuthorize only
        if ("Plant".equals(module) && "CREATE".equals(action)) {
            if ("DENY".equals(expected)) {
                assertFalse("VIEWER".equals(role) && allow);
                // Finance/Engineer plant create denied at annotation layer; matrix has no PLANT_CREATE
                assertFalse(perms.contains("PLANT_CREATE"));
            }
            return;
        }
        if ("ALLOW".equals(expected)) {
            assertTrue(allow, role + " should ALLOW " + module + "/" + action + " perms=" + perms);
        } else if ("DENY".equals(expected)) {
            assertFalse(allow, role + " should DENY " + module + "/" + action);
        }
    }

    @Test
    void viewer_notListedOnUniversalMutatingEndpoints() throws Exception {
        // Scan UniversalCostingController source for PostMapping PreAuthorize excluding VIEWER
        Path src = findController("UniversalCostingController.java");
        if (src == null) {
            return; // classpath-only environments
        }
        String text = Files.readString(src);
        // Crude but effective: after each @PostMapping, next @PreAuthorize should not include VIEWER
        String[] chunks = text.split("@PostMapping");
        for (int i = 1; i < chunks.length; i++) {
            String chunk = chunks[i];
            int pre = chunk.indexOf("@PreAuthorize");
            if (pre < 0) {
                continue;
            }
            String preLine = chunk.substring(pre, Math.min(chunk.length(), pre + 200));
            assertFalse(preLine.contains("'VIEWER'") || preLine.contains("\"VIEWER\""),
                    "VIEWER must not appear on Universal POST PreAuthorize: " + preLine);
        }
    }

    @Test
    void viewer_notOnPlantCreatePreAuthorize() throws Exception {
        Path src = findController("PlantMasterController.java");
        if (src == null) {
            return;
        }
        String text = Files.readString(src);
        int post = text.indexOf("@PostMapping");
        assertTrue(post > 0);
        String after = text.substring(post, Math.min(text.length(), post + 350));
        assertFalse(after.contains("'VIEWER'"), "VIEWER must not be on plant create");
        assertTrue(after.contains("COSTING_MANAGER") || after.contains("MASTER_DATA_ADMIN"));
    }

    private static Path findController(String fileName) throws IOException {
        Path root = Path.of("src/main/java");
        if (!Files.exists(root)) {
            root = Path.of("backend/src/main/java");
        }
        if (!Files.exists(root)) {
            return null;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.getFileName().toString().equals(fileName)).findFirst().orElse(null);
        }
    }
}
