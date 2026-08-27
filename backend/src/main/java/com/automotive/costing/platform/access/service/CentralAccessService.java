package com.automotive.costing.platform.access.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.dto.AccessDtos.AddMembershipRequest;
import com.automotive.costing.platform.access.dto.AccessDtos.CreateCustomerRequest;
import com.automotive.costing.platform.access.repository.PlatformCustomerRepository;
import com.automotive.costing.platform.access.dto.AccessDtos.UpdateCustomerRequest;
import com.automotive.costing.platform.access.dto.AccessDtos.SetCustomerStatusRequest;
import com.automotive.costing.platform.access.dto.AccessDtos.CustomerAccess;
import com.automotive.costing.platform.access.dto.AccessDtos.CustomerResponse;
import com.automotive.costing.platform.access.dto.AccessDtos.EffectiveAccessResponse;
import com.automotive.costing.platform.access.dto.AccessDtos.EntitlementResponse;
import com.automotive.costing.platform.access.dto.AccessDtos.MembershipResponse;
import com.automotive.costing.platform.access.dto.AccessDtos.ModuleResponse;
import com.automotive.costing.platform.access.dto.AccessDtos.SetEntitlementRequest;
import com.automotive.costing.platform.access.entity.CustomerModuleEntitlement;
import com.automotive.costing.platform.access.entity.CustomerStatus;
import com.automotive.costing.platform.access.entity.CustomerUserMembership;
import com.automotive.costing.platform.access.entity.MembershipStatus;
import com.automotive.costing.platform.access.entity.PlatformCustomer;
import com.automotive.costing.platform.access.entity.PlatformModule;
import com.automotive.costing.platform.access.repository.CustomerModuleEntitlementRepository;
import com.automotive.costing.platform.access.repository.CustomerFeatureEntitlementRepository;
import com.automotive.costing.platform.access.repository.FeatureCatalogRepository;
import com.automotive.costing.platform.access.entity.CustomerFeatureEntitlement;
import com.automotive.costing.platform.access.entity.FeatureCatalog;
import com.automotive.costing.platform.access.repository.CustomerUserMembershipRepository;
import com.automotive.costing.platform.access.repository.PlatformCustomerRepository;
import com.automotive.costing.platform.access.repository.PlatformModuleRepository;
import com.automotive.costing.platform.user.entity.Role;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stage A1 — Central Access Management: customer, module registry, entitlement, membership,
 * and effective access resolution for the current user.
 */
@Service
@RequiredArgsConstructor
public class CentralAccessService {

    public static final String MODULE_PRODUCT_COSTING = "PRODUCT_COSTING";
    public static final String MODULE_VENDOR_SELECTION = "VENDOR_SELECTION";

    private final PlatformCustomerRepository customerRepository;
    private final PlatformModuleRepository moduleRepository;
    private final CustomerModuleEntitlementRepository entitlementRepository;
    private final FeatureCatalogRepository featureCatalogRepository;
    private final CustomerFeatureEntitlementRepository featureEntitlementRepository;
    private final CustomerUserMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TenantCatalogSeedService tenantCatalogSeedService;
    private final CommercialLicenseService commercialLicenseService;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByCustomerCodeIgnoreCase(request.customerCode())) {
            throw new BusinessRuleViolationException("Customer code already exists", "ACCESS-01");
        }
        PlatformCustomer c = new PlatformCustomer();
        c.setCustomerId(UUID.randomUUID().toString());
        c.setCustomerCode(request.customerCode().trim().toUpperCase());
        c.setCustomerName(request.customerName().trim());
        c.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(c);
        // Seed catalog templates from DEFAULT customer (overhead + questions)
        customerRepository.findByCustomerCodeIgnoreCase("DEFAULT").ifPresent(def ->
                tenantCatalogSeedService.seedFromDefault(def.getCustomerId(), c.getCustomerId()));
        auditService.recordCreate("PlatformCustomer", c.getCustomerId(), c.getCustomerCode());
        return toCustomer(c);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> listCustomers() {
        return customerRepository.findAll().stream().map(this::toCustomer).toList();
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> listModules() {
        return moduleRepository.findAll().stream()
                .map(m -> new ModuleResponse(m.getModuleId(), m.getModuleCode(), m.getModuleName(),
                        m.getDescription(), m.getStatus().name()))
                .toList();
    }

    @Transactional
    public EntitlementResponse setEntitlement(SetEntitlementRequest request) {
        PlatformCustomer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", request.customerId()));
        PlatformModule module = moduleRepository.findByModuleCodeIgnoreCase(request.moduleCode())
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformModule", request.moduleCode()));

        CustomerModuleEntitlement e = entitlementRepository
                .findByCustomerIdAndModuleId(customer.getCustomerId(), module.getModuleId())
                .orElseGet(() -> {
                    CustomerModuleEntitlement n = new CustomerModuleEntitlement();
                    n.setEntitlementId(UUID.randomUUID().toString());
                    n.setCustomerId(customer.getCustomerId());
                    n.setModuleId(module.getModuleId());
                    return n;
                });
        e.setEnabled(request.enabled());
        e.setEffectiveFrom(request.effectiveFrom());
        e.setEffectiveTo(request.effectiveTo());
        entitlementRepository.save(e);
        auditService.recordUpdate("CustomerModuleEntitlement", e.getEntitlementId(), null,
                module.getModuleCode() + "=" + request.enabled());
        return toEntitlement(e, module.getModuleCode());
    }

    @Transactional
    public MembershipResponse addMembership(AddMembershipRequest request) {
        customerRepository.findById(request.customerId())
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", request.customerId()));
        userRepository.findById(request.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", request.userId()));

        CustomerUserMembership m = membershipRepository
                .findByCustomerIdAndUserId(request.customerId(), request.userId())
                .orElseGet(() -> {
                    CustomerUserMembership n = new CustomerUserMembership();
                    n.setMembershipId(UUID.randomUUID().toString());
                    n.setCustomerId(request.customerId());
                    n.setUserId(request.userId());
                    return n;
                });
        m.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(m);
        auditService.recordCreate("CustomerUserMembership", m.getMembershipId(),
                m.getCustomerId() + ":" + m.getUserId());
        return new MembershipResponse(m.getMembershipId(), m.getCustomerId(), m.getUserId(), m.getStatus().name());
    }

    /**
     * Effective access for the authenticated user: memberships + enabled modules + role codes.
     * If the user has no memberships, returns empty customer list (admin must assign).
     */
    @Transactional(readOnly = true)
    public EffectiveAccessResponse effectiveAccess(User user) {
        List<CustomerUserMembership> memberships =
                membershipRepository.findByUserIdAndStatus(user.getUserId(), MembershipStatus.ACTIVE);
        List<String> roleCodes = user.getRoles() == null ? List.of()
                : user.getRoles().stream().map(Role::getRoleCode).sorted().collect(Collectors.toList());

        List<CustomerAccess> customers = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (CustomerUserMembership membership : memberships) {
            PlatformCustomer customer = customerRepository.findById(membership.getCustomerId()).orElse(null);
            if (customer == null || customer.getStatus() != CustomerStatus.ACTIVE) {
                continue;
            }
            List<String> modules = new ArrayList<>();
            for (CustomerModuleEntitlement e : entitlementRepository.findByCustomerId(customer.getCustomerId())) {
                if (!e.isEnabled()) {
                    continue;
                }
                if (e.getEffectiveFrom() != null && today.isBefore(e.getEffectiveFrom())) {
                    continue;
                }
                if (e.getEffectiveTo() != null && today.isAfter(e.getEffectiveTo())) {
                    continue;
                }
                moduleRepository.findById(e.getModuleId()).ifPresent(mod -> modules.add(mod.getModuleCode()));
            }
            modules.sort(String::compareTo);
            customers.add(new CustomerAccess(
                    customer.getCustomerId(),
                    customer.getCustomerCode(),
                    customer.getCustomerName(),
                    modules,
                    roleCodes));
        }
        return new EffectiveAccessResponse(user.getUserId(), user.getUsername(), customers);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(String customerId) {
        PlatformCustomer c = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", customerId));
        return new CustomerResponse(c.getCustomerId(), c.getCustomerCode(), c.getCustomerName(), c.getStatus().name());
    }

    @Transactional(readOnly = true)
    public List<EntitlementResponse> listEntitlements(String customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", customerId));
        return entitlementRepository.findByCustomerId(customerId).stream()
                .map(e -> {
                    PlatformModule mod = moduleRepository.findById(e.getModuleId()).orElse(null);
                    return new EntitlementResponse(
                            e.getEntitlementId(),
                            e.getCustomerId(),
                            e.getModuleId(),
                            mod != null ? mod.getModuleCode() : null,
                            e.isEnabled(),
                            e.getEffectiveFrom(),
                            e.getEffectiveTo());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> listMemberships(String customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", customerId));
        return membershipRepository.findByCustomerId(customerId).stream()
                .map(m -> new MembershipResponse(
                        m.getMembershipId(), m.getCustomerId(), m.getUserId(), m.getStatus().name()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public void assertModuleEnabled(String customerId, String moduleCode) {
        PlatformModule module = moduleRepository.findByModuleCodeIgnoreCase(moduleCode)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformModule", moduleCode));
        CustomerModuleEntitlement e = entitlementRepository
                .findByCustomerIdAndModuleId(customerId, module.getModuleId())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Module " + moduleCode + " is not entitled for this customer", "ACCESS-02"));
        if (!e.isEnabled()) {
            throw new BusinessRuleViolationException(
                    "Module " + moduleCode + " is disabled for this customer", "ACCESS-03");
        }
        LocalDate today = LocalDate.now();
        if (e.getEffectiveFrom() != null && today.isBefore(e.getEffectiveFrom())) {
            throw new BusinessRuleViolationException("Module entitlement not yet effective", "ACCESS-04");
        }
        if (e.getEffectiveTo() != null && today.isAfter(e.getEffectiveTo())) {
            throw new BusinessRuleViolationException("Module entitlement expired", "ACCESS-05");
        }
    }

    @Transactional(readOnly = true)
    public void assertUserInCustomer(String customerId, String userId) {
        PlatformCustomer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", customerId));
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Customer is " + customer.getStatus().name() + " — access denied", "ACCESS-09");
        }
        if (!membershipRepository.existsByCustomerIdAndUserIdAndStatus(
                customerId, userId, MembershipStatus.ACTIVE)) {
            throw new BusinessRuleViolationException(
                    "User is not an active member of this customer", "ACCESS-06");
        }
    }

    /**
     * Platform control-plane users must not use X-Customer-Id for business modules
     * unless they hold an active membership or an approved support session.
     */
    @Transactional(readOnly = true)
    public void assertCustomerActive(String customerId) {
        PlatformCustomer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", customerId));
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Customer is " + customer.getStatus().name() + " — access denied", "ACCESS-09");
        }
    }

    public boolean hasActiveMembership(String customerId, String userId) {
        return membershipRepository.existsByCustomerIdAndUserIdAndStatus(
                customerId, userId, MembershipStatus.ACTIVE);
    }

    @Transactional
    public void invalidateCustomerUserTokens(String customerId) {
        List<CustomerUserMembership> members = membershipRepository.findByCustomerId(customerId);
        for (CustomerUserMembership m : members) {
            userRepository.findById(m.getUserId()).ifPresent(u -> {
                u.setTokenVersion(u.getTokenVersion() + 1);
                userRepository.save(u);
            });
        }
    }


    @Transactional
    public CustomerResponse updateCustomer(String customerId, UpdateCustomerRequest request) {
        PlatformCustomer c = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", customerId));
        String previous = c.getCustomerName();
        c.setCustomerName(request.customerName().trim());
        customerRepository.save(c);
        auditService.recordUpdate("PlatformCustomer", c.getCustomerId(), previous, c.getCustomerName());
        return toCustomer(c);
    }

    @Transactional
    public CustomerResponse setCustomerStatus(String customerId, SetCustomerStatusRequest request) {
        PlatformCustomer c = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", customerId));
        CustomerStatus target;
        try {
            target = CustomerStatus.valueOf(request.status().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(
                    "Invalid customer status: " + request.status(), "ACCESS-08");
        }
        if (c.getStatus() == target) {
            return toCustomer(c);
        }
        CustomerStatus previous = c.getStatus();
        c.setStatus(target);
        customerRepository.save(c);
        auditService.recordTransition("PlatformCustomer", c.getCustomerId(), previous.name(), target.name());
        return toCustomer(c);
    }

    @Transactional
    public MembershipResponse deactivateMembership(String membershipId) {
        CustomerUserMembership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerUserMembership", membershipId));
        if (m.getStatus() == MembershipStatus.INACTIVE) {
            return new MembershipResponse(m.getMembershipId(), m.getCustomerId(), m.getUserId(), m.getStatus().name());
        }
        MembershipStatus previous = m.getStatus();
        m.setStatus(MembershipStatus.INACTIVE);
        membershipRepository.save(m);
        auditService.recordTransition("CustomerUserMembership", m.getMembershipId(),
                previous.name(), MembershipStatus.INACTIVE.name());
        return new MembershipResponse(m.getMembershipId(), m.getCustomerId(), m.getUserId(), m.getStatus().name());
    }


    public record PlatformDashboardSummary(
            long totalCustomers,
            long activeCustomers,
            long suspendedCustomers,
            long inactiveCustomers,
            long totalMemberships
    ) {}


    /**
     * Feature entitlement is subordinate to module entitlement.
     * Order: module must be enabled, feature catalog active, customer feature entitlement enabled.
     */
    public void assertFeatureEnabled(String customerId, String featureCode) {
        FeatureCatalog feature = featureCatalogRepository.findByFeatureCodeIgnoreCase(featureCode)
                .orElseThrow(() -> ResourceNotFoundException.of("FeatureCatalog", featureCode));
        if (!feature.isActive()) {
            throw new BusinessRuleViolationException(
                    "Feature " + featureCode + " is not active in catalog", "ACCESS-FEAT-02");
        }
        assertModuleEnabled(customerId, feature.getModuleCode());

        // V281: FTA_TRADE_AGREEMENT requires IMPORT_COSTING (platform dependency)
        if ("FTA_TRADE_AGREEMENT".equalsIgnoreCase(featureCode)) {
            assertFeatureEnabled(customerId, "IMPORT_COSTING");
        }

        CustomerFeatureEntitlement e = featureEntitlementRepository
                .findByCustomerIdAndFeatureId(customerId, feature.getFeatureId())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Feature " + featureCode + " is not entitled for this customer", "ACCESS-FEAT-03"));
        if (!e.isEnabled()) {
            throw new BusinessRuleViolationException(
                    "Feature " + featureCode + " is disabled for this customer", "ACCESS-FEAT-04");
        }
        LocalDate today = LocalDate.now();
        if (e.getEffectiveFrom() != null && today.isBefore(e.getEffectiveFrom())) {
            throw new BusinessRuleViolationException(
                    "Feature entitlement not yet effective", "ACCESS-FEAT-05");
        }
        if (e.getEffectiveTo() != null && today.isAfter(e.getEffectiveTo())) {
            throw new BusinessRuleViolationException(
                    "Feature entitlement has expired", "ACCESS-FEAT-06");
        }
    }

    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(String customerId, String featureCode) {
        try {
            assertFeatureEnabled(customerId, featureCode);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public PlatformDashboardSummary platformDashboardSummary() {
        var all = customerRepository.findAll();
        long active = all.stream().filter(c -> c.getStatus() == CustomerStatus.ACTIVE).count();
        long sus = all.stream().filter(c -> c.getStatus() == CustomerStatus.SUSPENDED).count();
        long ina = all.stream().filter(c -> c.getStatus() == CustomerStatus.INACTIVE).count();
        long memberships = membershipRepository.count();
        return new PlatformDashboardSummary(all.size(), active, sus, ina, memberships);
    }

    private CustomerResponse toCustomer(PlatformCustomer c) {
        return new CustomerResponse(c.getCustomerId(), c.getCustomerCode(), c.getCustomerName(), c.getStatus().name());
    }

    private EntitlementResponse toEntitlement(CustomerModuleEntitlement e, String moduleCode) {
        return new EntitlementResponse(e.getEntitlementId(), e.getCustomerId(), e.getModuleId(),
                moduleCode, e.isEnabled(), e.getEffectiveFrom(), e.getEffectiveTo());
    }

    /** Commercial license must be operational for mutating business operations. */
    public void assertLicenseActive(String customerId) {
        commercialLicenseService.assertLicenseActive(customerId);
    }

    public void assertTotalUserLimit(String customerId, int additional) {
        commercialLicenseService.assertTotalUserLimit(customerId, additional);
    }

    public void assertRoleUserLimit(String customerId, String roleCode, int additional) {
        commercialLicenseService.assertRoleUserLimit(customerId, roleCode, additional);
    }


    public void assertLicenseActiveIfLicensed(String customerId) {
        commercialLicenseService.assertLicenseActiveIfLicensed(customerId);
    }

    public void assertPlantLimit(String customerId, int additional) {
        commercialLicenseService.assertPlantLimit(customerId, additional);
    }

}
