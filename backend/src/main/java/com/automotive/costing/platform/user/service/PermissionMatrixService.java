package com.automotive.costing.platform.user.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full permission matrix — logical capability catalogue mapped from role codes.
 * <p>
 * Authorization remains role-based ({@code @PreAuthorize} + module entitlement).
 * This service does <strong>not</strong> invent a separate permission persistence model;
 * it provides an explicit, testable matrix for UI display, Customer Admin guidance,
 * and regression tests (V224).
 * </p>
 */
@Service
public class PermissionMatrixService {

    public record PermissionDef(String code, String group, String description) {}

    public record RolePermissionView(
            String roleCode,
            String description,
            List<String> grantedPermissions,
            List<String> deniedHighlights
    ) {}

    public record PermissionMatrixResponse(
            List<PermissionDef> catalogue,
            List<RolePermissionView> roles
    ) {}

    /** Platform-only permissions Customer Admin must never grant. */
    public static final Set<String> PLATFORM_ONLY_PERMISSIONS = Set.of(
            "PLATFORM_ADMIN",
            "TENANT_MANAGE",
            "SUBSCRIPTION_MANAGE",
            "SUPPORT_ACCESS_REQUEST",
            "USER_MANAGE_ALL_TENANTS"
    );

    /** Roles Customer Admin may assign within their tenant. */
    public static final Set<String> CUSTOMER_ADMIN_ASSIGNABLE_ROLES = Set.of(
            "CUSTOMER_ADMIN",
            "COSTING_ENGINEER",
            "COSTING_MANAGER",
            "PURCHASING_BUYER",
            "SOURCING_LEAD",
            "MASTER_DATA_ADMIN",
            "FINANCE_CONTROLLER",
            "PROGRAM_COST_MANAGER",
            "PROCESS_ENGINEER",
            "TOOLING_ENGINEER",
            "PRODUCT_COST_ENGINEER",
            "COMMODITY_MATERIAL_BUYER",
            "INDUSTRIAL_ENGINEER",
            "PURCHASING_MANAGER",
            "VIEWER"
    );

    /** Roles that must never be assigned by Customer Admin. */
    public static final Set<String> CUSTOMER_ADMIN_FORBIDDEN_ROLES = Set.of(
            "PLATFORM_OWNER",
            "SYSTEM_ADMINISTRATOR",
            "ADMIN",
            "APPROVAL_OVERRIDE"
    );

    private static final List<PermissionDef> CATALOGUE = List.of(
            new PermissionDef("BOM_VIEW", "Product Costing", "View BOM structure"),
            new PermissionDef("BOM_CREATE", "Product Costing", "Create BOM"),
            new PermissionDef("BOM_EDIT", "Product Costing", "Edit BOM"),
            new PermissionDef("BOM_DELETE", "Product Costing", "Delete BOM"),
            new PermissionDef("COSTING_VIEW", "Product Costing", "View cost estimates"),
            new PermissionDef("COSTING_CREATE", "Product Costing", "Create costing"),
            new PermissionDef("COSTING_EDIT", "Product Costing", "Edit costing"),
            new PermissionDef("COSTING_APPROVE", "Product Costing", "Approve costing"),
            new PermissionDef("SHOULD_COST", "Product Costing", "Should-cost analysis"),
            new PermissionDef("MAKE_VS_BUY", "Product Costing", "Make vs Buy"),
            new PermissionDef("RFQ_VIEW", "Sourcing", "View RFQs"),
            new PermissionDef("RFQ_CREATE", "Sourcing", "Create RFQ"),
            new PermissionDef("RFQ_EDIT", "Sourcing", "Edit RFQ"),
            new PermissionDef("RFQ_DELETE", "Sourcing", "Delete RFQ"),
            new PermissionDef("SUPPLIER_VIEW", "Sourcing", "View suppliers"),
            new PermissionDef("SUPPLIER_CREATE", "Sourcing", "Create supplier"),
            new PermissionDef("SUPPLIER_EDIT", "Sourcing", "Edit supplier"),
            new PermissionDef("QUOTE_VIEW", "Sourcing", "View supplier quotes"),
            new PermissionDef("QUOTE_CREATE", "Sourcing", "Create quote"),
            new PermissionDef("QUOTE_EDIT", "Sourcing", "Edit quote"),
            new PermissionDef("REPORT_VIEW", "Reports", "View reports"),
            new PermissionDef("DASHBOARD_VIEW", "Reports", "View dashboards"),
            new PermissionDef("USER_VIEW", "Administration", "View users in own tenant"),
            new PermissionDef("USER_CREATE", "Administration", "Create users in own tenant"),
            new PermissionDef("USER_EDIT", "Administration", "Edit users in own tenant"),
            new PermissionDef("USER_DISABLE", "Administration", "Activate/deactivate users in own tenant"),
            new PermissionDef("ROLE_VIEW", "Administration", "View roles"),
            new PermissionDef("ROLE_MANAGE", "Administration", "Manage custom roles (limited)"),
            new PermissionDef("AUDIT_VIEW", "Administration", "View audit log (own tenant scope)"),
            new PermissionDef("SUBSCRIPTION_VIEW", "Platform", "View subscription"),
            new PermissionDef("SUBSCRIPTION_MANAGE", "Platform", "Manage subscription"),
            new PermissionDef("PLATFORM_ADMIN", "Platform", "Platform administration"),
            new PermissionDef("TENANT_MANAGE", "Platform", "Create/suspend customers"),
            new PermissionDef("SUPPORT_ACCESS_REQUEST", "Platform", "Request support access"),
            new PermissionDef("USER_MANAGE_ALL_TENANTS", "Platform", "Manage users across all tenants")
    );

    private static final Map<String, List<String>> ROLE_GRANTS = buildRoleGrants();

    private static Map<String, List<String>> buildRoleGrants() {
        Map<String, List<String>> m = new LinkedHashMap<>();

        m.put("PLATFORM_OWNER", List.of(
                "PLATFORM_ADMIN", "TENANT_MANAGE", "SUBSCRIPTION_VIEW", "SUBSCRIPTION_MANAGE",
                "SUPPORT_ACCESS_REQUEST", "DASHBOARD_VIEW", "REPORT_VIEW", "AUDIT_VIEW"
        ));

        m.put("SYSTEM_ADMINISTRATOR", List.of(
                "PLATFORM_ADMIN", "TENANT_MANAGE", "SUBSCRIPTION_VIEW", "SUBSCRIPTION_MANAGE",
                "SUPPORT_ACCESS_REQUEST", "USER_MANAGE_ALL_TENANTS",
                "USER_VIEW", "USER_CREATE", "USER_EDIT", "USER_DISABLE",
                "ROLE_VIEW", "ROLE_MANAGE", "AUDIT_VIEW",
                "BOM_VIEW", "BOM_CREATE", "BOM_EDIT", "BOM_DELETE",
                "COSTING_VIEW", "COSTING_CREATE", "COSTING_EDIT", "COSTING_APPROVE",
                "SHOULD_COST", "MAKE_VS_BUY",
                "RFQ_VIEW", "RFQ_CREATE", "RFQ_EDIT", "RFQ_DELETE",
                "SUPPLIER_VIEW", "SUPPLIER_CREATE", "SUPPLIER_EDIT",
                "QUOTE_VIEW", "QUOTE_CREATE", "QUOTE_EDIT",
                "REPORT_VIEW", "DASHBOARD_VIEW"
        ));

        m.put("CUSTOMER_ADMIN", List.of(
                "USER_VIEW", "USER_CREATE", "USER_EDIT", "USER_DISABLE",
                "ROLE_VIEW", "AUDIT_VIEW", "DASHBOARD_VIEW", "REPORT_VIEW",
                "BOM_VIEW", "COSTING_VIEW", "RFQ_VIEW", "SUPPLIER_VIEW", "QUOTE_VIEW"
        ));

        m.put("COSTING_ENGINEER", List.of(
                "BOM_VIEW", "BOM_CREATE", "BOM_EDIT",
                "COSTING_VIEW", "COSTING_CREATE", "COSTING_EDIT",
                "SHOULD_COST", "MAKE_VS_BUY",
                "DASHBOARD_VIEW", "REPORT_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("COSTING_MANAGER", List.of(
                "BOM_VIEW", "BOM_CREATE", "BOM_EDIT",
                "COSTING_VIEW", "COSTING_CREATE", "COSTING_EDIT", "COSTING_APPROVE",
                "SHOULD_COST", "MAKE_VS_BUY",
                "RFQ_VIEW", "SUPPLIER_VIEW", "QUOTE_VIEW",
                "DASHBOARD_VIEW", "REPORT_VIEW", "AUDIT_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("PURCHASING_BUYER", List.of(
                "RFQ_VIEW", "RFQ_CREATE", "RFQ_EDIT",
                "SUPPLIER_VIEW", "QUOTE_VIEW", "QUOTE_CREATE", "QUOTE_EDIT",
                "COSTING_VIEW", "DASHBOARD_VIEW", "REPORT_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("SOURCING_LEAD", List.of(
                "RFQ_VIEW", "RFQ_CREATE", "RFQ_EDIT", "RFQ_DELETE",
                "SUPPLIER_VIEW", "SUPPLIER_CREATE", "SUPPLIER_EDIT",
                "QUOTE_VIEW", "QUOTE_CREATE", "QUOTE_EDIT",
                "COSTING_VIEW", "SHOULD_COST",
                "DASHBOARD_VIEW", "REPORT_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("MASTER_DATA_ADMIN", List.of(
                "BOM_VIEW", "BOM_CREATE", "BOM_EDIT",
                "COSTING_VIEW", "DASHBOARD_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("FINANCE_CONTROLLER", List.of(
                "COSTING_VIEW", "COSTING_APPROVE",
                "REPORT_VIEW", "DASHBOARD_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("PROGRAM_COST_MANAGER", List.of(
                "COSTING_VIEW", "SHOULD_COST", "MAKE_VS_BUY",
                "REPORT_VIEW", "DASHBOARD_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("PROCESS_ENGINEER", List.of(
                "BOM_VIEW", "COSTING_VIEW", "SHOULD_COST", "DASHBOARD_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("TOOLING_ENGINEER", List.of(
                "BOM_VIEW", "COSTING_VIEW", "SHOULD_COST", "DASHBOARD_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("PRODUCT_COST_ENGINEER", List.of(
                "BOM_VIEW", "COSTING_VIEW", "SHOULD_COST", "DASHBOARD_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("COMMODITY_MATERIAL_BUYER", List.of(
                "COSTING_VIEW", "BOM_VIEW",
                "RFQ_VIEW", "RFQ_CREATE",
                "DASHBOARD_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("INDUSTRIAL_ENGINEER", List.of(
                "BOM_VIEW", "COSTING_VIEW", "SHOULD_COST", "DASHBOARD_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("PURCHASING_MANAGER", List.of(
                "RFQ_VIEW", "RFQ_CREATE", "RFQ_EDIT",
                "COSTING_VIEW", "COSTING_APPROVE",
                "SUPPLIER_VIEW", "QUOTE_VIEW",
                "DASHBOARD_VIEW", "REPORT_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("VIEWER", List.of(
                "BOM_VIEW", "COSTING_VIEW", "RFQ_VIEW", "SUPPLIER_VIEW", "QUOTE_VIEW",
                "REPORT_VIEW", "DASHBOARD_VIEW",
                "SUPPORT_TICKET_READ", "SUPPORT_TICKET_CREATE", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET_CLOSE"));

        m.put("ADMIN", m.get("SYSTEM_ADMINISTRATOR")); // alias overlap documented in FE

        return m;
    }

    public PermissionMatrixResponse fullMatrix() {
        List<RolePermissionView> roles = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : ROLE_GRANTS.entrySet()) {
            List<String> granted = e.getValue();
            List<String> denied = new ArrayList<>();
            for (String p : PLATFORM_ONLY_PERMISSIONS) {
                if (!granted.contains(p)) {
                    denied.add(p);
                }
            }
            roles.add(new RolePermissionView(
                    e.getKey(),
                    roleDescription(e.getKey()),
                    granted,
                    denied
            ));
        }
        return new PermissionMatrixResponse(CATALOGUE, roles);
    }

    public boolean isAssignableByCustomerAdmin(String roleCode) {
        return CUSTOMER_ADMIN_ASSIGNABLE_ROLES.contains(roleCode)
                && !CUSTOMER_ADMIN_FORBIDDEN_ROLES.contains(roleCode);
    }

    public boolean isPlatformOnlyPermission(String permissionCode) {
        return PLATFORM_ONLY_PERMISSIONS.contains(permissionCode);
    }

    private static String roleDescription(String code) {
        return switch (code) {
            case "PLATFORM_OWNER" -> "Platform owner — tenant/subscription control plane only";
            case "SYSTEM_ADMINISTRATOR" -> "Full system administration";
            case "ADMIN" -> "Administrative alias overlapping system admin for CAM";
            case "CUSTOMER_ADMIN" -> "Customer administrator — own tenant users only";
            case "COSTING_ENGINEER" -> "Builds BOMs, routings, should-cost estimates";
            case "COSTING_MANAGER" -> "Reviews/approves cost estimates";
            case "PURCHASING_BUYER" -> "Runs RFQs, negotiates with suppliers";
            case "SOURCING_LEAD" -> "Cross-supplier visibility, approves awards";
            case "MASTER_DATA_ADMIN" -> "Create/edit master data records";
            case "FINANCE_CONTROLLER" -> "Approves/maintains rates and financial controls";
            case "PROGRAM_COST_MANAGER" -> "Program-level target costing / VA-VE";
            case "PROCESS_ENGINEER" -> "Process routings and cycle times";
            case "TOOLING_ENGINEER" -> "Tooling investment estimates";
            case "PRODUCT_COST_ENGINEER" -> "Builds/reviews should-cost and challenges supplier quotations";
            case "COMMODITY_MATERIAL_BUYER" -> "Material sourcing and material cost control";
            case "INDUSTRIAL_ENGINEER" -> "Process routing, cycle time and line balancing";
            case "PURCHASING_MANAGER" -> "Purchasing approvals, spend and buyer workload";
            case "VIEWER" -> "Read-only access to approved surfaces";
            default -> code;
        };
    }

    public java.util.List<String> permissionsForRole(String roleCode) {
        String role = roleCode != null && roleCode.startsWith("ROLE_") ? roleCode.substring(5) : roleCode;
        return ROLE_GRANTS.getOrDefault(role, java.util.List.of());
    }

    public boolean hasPermissionForRole(String roleCode, String permissionCode) {
        return permissionsForRole(roleCode).contains(permissionCode);
    }

    public boolean hasPermission(java.util.Collection<String> roleCodes, String permissionCode) {
        if (roleCodes == null || permissionCode == null || permissionCode.isBlank()) return false;
        for (String r : roleCodes) {
            if (r != null && hasPermissionForRole(r, permissionCode)) return true;
        }
        return false;
    }

}
