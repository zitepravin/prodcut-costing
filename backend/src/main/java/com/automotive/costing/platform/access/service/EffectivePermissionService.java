package com.automotive.costing.platform.access.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.entity.CustomerPermissionEntitlement;
import com.automotive.costing.platform.access.entity.CustomerRolePermission;
import com.automotive.costing.platform.access.entity.PermissionCatalogEntry;
import com.automotive.costing.platform.access.repository.CustomerPermissionEntitlementRepository;
import com.automotive.costing.platform.access.repository.CustomerRolePermissionRepository;
import com.automotive.costing.platform.access.repository.PermissionCatalogEntryRepository;
import com.automotive.costing.platform.access.repository.PlatformCustomerRepository;
import com.automotive.costing.platform.user.service.PermissionMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 2 effective access:
 * platformEntitlement(customer, permission) AND rolePermission(customer, roles, permission)
 * Missing platform row → ALLOW (backward compatible).
 * Missing role override → fall back to static PermissionMatrixService ROLE_GRANTS.
 */
@Service
@RequiredArgsConstructor
public class EffectivePermissionService {

    private final PermissionCatalogEntryRepository catalogRepository;
    private final CustomerPermissionEntitlementRepository platformEntitlementRepository;
    private final CustomerRolePermissionRepository rolePermissionRepository;
    private final PlatformCustomerRepository customerRepository;
    private final PermissionMatrixService permissionMatrixService;
    private final AuditService auditService;

    public record CatalogRow(
            String permissionCode, String moduleCode, String featureCode,
            String functionCode, String functionName, String description) {}

    public record EntitlementRow(
            String permissionCode, String moduleCode, String featureCode,
            String functionName, boolean platformAllowed, boolean locked) {}

    public record RolePermissionRow(
            String permissionCode, String moduleCode, String featureCode,
            String functionName, boolean platformAllowed, boolean roleAllowed,
            boolean locked, boolean effectiveAllowed) {}

    @Transactional(readOnly = true)
    public List<CatalogRow> listCatalog() {
        return catalogRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(e -> new CatalogRow(
                        e.getPermissionCode(), e.getModuleCode(), e.getFeatureCode(),
                        e.getFunctionCode(), e.getFunctionName(), e.getDescription()))
                .toList();
    }

    /** Platform entitlement matrix for a customer (SYSTEM_ADMINISTRATOR). */
    @Transactional(readOnly = true)
    public List<EntitlementRow> listPlatformEntitlements(String customerId) {
        requireCustomer(customerId);
        Map<String, Boolean> overrides = platformEntitlementRepository.findByCustomerId(customerId).stream()
                .collect(Collectors.toMap(CustomerPermissionEntitlement::getPermissionCode,
                        CustomerPermissionEntitlement::isAllowed, (a, b) -> a));
        return catalogRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(e -> {
                    boolean allowed = overrides.getOrDefault(e.getPermissionCode(), true);
                    return new EntitlementRow(
                            e.getPermissionCode(), e.getModuleCode(), e.getFeatureCode(),
                            e.getFunctionName(), allowed, false);
                })
                .toList();
    }

    @Transactional
    public EntitlementRow setPlatformEntitlement(String customerId, String permissionCode, boolean allowed) {
        requireCustomer(customerId);
        PermissionCatalogEntry cat = catalogRepository.findById(permissionCode)
                .orElseThrow(() -> ResourceNotFoundException.of("PermissionCatalogEntry", permissionCode));
        CustomerPermissionEntitlement row = platformEntitlementRepository
                .findByCustomerIdAndPermissionCode(customerId, permissionCode)
                .orElseGet(() -> {
                    CustomerPermissionEntitlement n = new CustomerPermissionEntitlement();
                    n.setEntitlementId(UUID.randomUUID().toString());
                    n.setCustomerId(customerId);
                    n.setPermissionCode(permissionCode);
                    return n;
                });
        boolean old = row.isAllowed();
        row.setAllowed(allowed);
        platformEntitlementRepository.save(row);
        auditService.recordUpdate("CustomerPermissionEntitlement", customerId + ":" + permissionCode,
                Map.of("allowed", old), Map.of("allowed", allowed));
        return new EntitlementRow(permissionCode, cat.getModuleCode(), cat.getFeatureCode(),
                cat.getFunctionName(), allowed, false);
    }

    /** Customer Admin view: role permissions with platform lock. */
    @Transactional(readOnly = true)
    public List<RolePermissionRow> listRolePermissions(String customerId, String roleCode) {
        requireCustomer(customerId);
        String role = normalizeRole(roleCode);
        Map<String, Boolean> platform = platformEntitlementRepository.findByCustomerId(customerId).stream()
                .collect(Collectors.toMap(CustomerPermissionEntitlement::getPermissionCode,
                        CustomerPermissionEntitlement::isAllowed, (a, b) -> a));
        Map<String, Boolean> roleOverrides = rolePermissionRepository
                .findByCustomerIdAndRoleCode(customerId, role).stream()
                .collect(Collectors.toMap(CustomerRolePermission::getPermissionCode,
                        CustomerRolePermission::isAllowed, (a, b) -> a));
        List<String> matrixDefault = permissionMatrixService.permissionsForRole(role);

        return catalogRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(e -> {
                    boolean platformAllowed = platform.getOrDefault(e.getPermissionCode(), true);
                    boolean matrixAllows = matrixDefault.contains(e.getPermissionCode());
                    boolean roleAllowed = roleOverrides.getOrDefault(e.getPermissionCode(), matrixAllows);
                    boolean locked = !platformAllowed;
                    boolean effective = platformAllowed && roleAllowed;
                    return new RolePermissionRow(
                            e.getPermissionCode(), e.getModuleCode(), e.getFeatureCode(),
                            e.getFunctionName(), platformAllowed, roleAllowed, locked, effective);
                })
                .toList();
    }

    @Transactional
    public RolePermissionRow setRolePermission(String customerId, String roleCode, String permissionCode, boolean allowed) {
        requireCustomer(customerId);
        String role = normalizeRole(roleCode);
        PermissionCatalogEntry cat = catalogRepository.findById(permissionCode)
                .orElseThrow(() -> ResourceNotFoundException.of("PermissionCatalogEntry", permissionCode));

        boolean platformAllowed = platformEntitlementRepository
                .findByCustomerIdAndPermissionCode(customerId, permissionCode)
                .map(CustomerPermissionEntitlement::isAllowed)
                .orElse(true);
        if (!platformAllowed && allowed) {
            throw new BusinessRuleViolationException(
                    "Platform has denied " + permissionCode + " for this customer; cannot ALLOW at role level",
                    "ENTITLEMENT-01");
        }

        CustomerRolePermission row = rolePermissionRepository
                .findByCustomerIdAndRoleCodeAndPermissionCode(customerId, role, permissionCode)
                .orElseGet(() -> {
                    CustomerRolePermission n = new CustomerRolePermission();
                    n.setGrantId(UUID.randomUUID().toString());
                    n.setCustomerId(customerId);
                    n.setRoleCode(role);
                    n.setPermissionCode(permissionCode);
                    return n;
                });
        boolean old = row.isAllowed();
        row.setAllowed(allowed);
        rolePermissionRepository.save(row);
        auditService.recordUpdate("CustomerRolePermission",
                customerId + ":" + role + ":" + permissionCode,
                Map.of("allowed", old), Map.of("allowed", allowed));

        boolean effective = platformAllowed && allowed;
        return new RolePermissionRow(permissionCode, cat.getModuleCode(), cat.getFeatureCode(),
                cat.getFunctionName(), platformAllowed, allowed, !platformAllowed, effective);
    }

    /**
     * Effective check for current tenant + caller roles.
     * Used by {@code @permissionAuth} and services.
     */
    @Transactional(readOnly = true)
    public boolean hasEffectivePermission(Collection<String> roleCodes, String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        String customerId = CustomerContext.getCustomerId();
        boolean platformAllowed = true;
        if (customerId != null && !customerId.isBlank()) {
            platformAllowed = platformEntitlementRepository
                    .findByCustomerIdAndPermissionCode(customerId, permissionCode)
                    .map(CustomerPermissionEntitlement::isAllowed)
                    .orElse(true);
        }
        if (!platformAllowed) {
            return false;
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        for (String raw : roleCodes) {
            if (raw == null) continue;
            String role = normalizeRole(raw);
            Optional<CustomerRolePermission> override = customerId == null || customerId.isBlank()
                    ? Optional.empty()
                    : rolePermissionRepository.findByCustomerIdAndRoleCodeAndPermissionCode(
                            customerId, role, permissionCode);
            if (override.isPresent()) {
                if (override.get().isAllowed()) {
                    return true;
                }
            } else if (permissionMatrixService.hasPermissionForRole(role, permissionCode)) {
                return true;
            }
        }
        return false;
    }

    private void requireCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new BusinessRuleViolationException("customerId is required", "ACCESS-10");
        }
        if (!customerRepository.existsById(customerId)) {
            throw ResourceNotFoundException.of("PlatformCustomer", customerId);
        }
    }

    private static String normalizeRole(String role) {
        return role != null && role.startsWith("ROLE_") ? role.substring(5) : role;
    }
}
