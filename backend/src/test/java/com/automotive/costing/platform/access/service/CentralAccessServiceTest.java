package com.automotive.costing.platform.access.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.dto.AccessDtos.CreateCustomerRequest;
import com.automotive.costing.platform.access.dto.AccessDtos.SetEntitlementRequest;
import com.automotive.costing.platform.access.entity.CustomerModuleEntitlement;
import com.automotive.costing.platform.access.entity.CustomerStatus;
import com.automotive.costing.platform.access.entity.CustomerUserMembership;
import com.automotive.costing.platform.access.entity.MembershipStatus;
import com.automotive.costing.platform.access.entity.ModuleStatus;
import com.automotive.costing.platform.access.entity.PlatformCustomer;
import com.automotive.costing.platform.access.entity.PlatformModule;
import com.automotive.costing.platform.access.repository.CustomerModuleEntitlementRepository;
import com.automotive.costing.platform.access.repository.CustomerUserMembershipRepository;
import com.automotive.costing.platform.access.repository.PlatformCustomerRepository;
import com.automotive.costing.platform.access.repository.PlatformModuleRepository;
import com.automotive.costing.platform.user.entity.Role;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import com.automotive.costing.platform.access.entity.MembershipStatus;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class CentralAccessServiceTest {

    @Mock private PlatformCustomerRepository customerRepository;
    @Mock private PlatformModuleRepository moduleRepository;
    @Mock private CustomerModuleEntitlementRepository entitlementRepository;
    @Mock private com.automotive.costing.platform.access.repository.FeatureCatalogRepository featureCatalogRepository;
    @Mock private com.automotive.costing.platform.access.repository.CustomerFeatureEntitlementRepository featureEntitlementRepository;
    @Mock private CustomerUserMembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private com.automotive.costing.platform.access.service.TenantCatalogSeedService tenantCatalogSeedService;

    @InjectMocks
    private CentralAccessService service;

    @Test
    void createCustomer_rejectsDuplicateCode() {
        when(customerRepository.existsByCustomerCodeIgnoreCase("ACME")).thenReturn(true);
        assertThatThrownBy(() -> service.createCustomer(new CreateCustomerRequest("ACME", "Acme")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void effectiveAccess_returnsEnabledModules() {
        User user = new User();
        user.setUserId("u1");
        user.setUsername("alice");
        Role role = new Role();
        role.setRoleCode("COSTING_ENGINEER");
        user.setRoles(Set.of(role));

        CustomerUserMembership mem = new CustomerUserMembership();
        mem.setCustomerId("c1");
        mem.setUserId("u1");
        mem.setStatus(MembershipStatus.ACTIVE);
        when(membershipRepository.findByUserIdAndStatus("u1", MembershipStatus.ACTIVE))
                .thenReturn(List.of(mem));

        PlatformCustomer customer = new PlatformCustomer();
        customer.setCustomerId("c1");
        customer.setCustomerCode("ACME");
        customer.setCustomerName("Acme");
        customer.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findById("c1")).thenReturn(Optional.of(customer));

        CustomerModuleEntitlement ent = new CustomerModuleEntitlement();
        ent.setModuleId("m1");
        ent.setEnabled(true);
        when(entitlementRepository.findByCustomerId("c1")).thenReturn(List.of(ent));

        PlatformModule mod = new PlatformModule();
        mod.setModuleId("m1");
        mod.setModuleCode("PRODUCT_COSTING");
        mod.setStatus(ModuleStatus.ACTIVE);
        when(moduleRepository.findById("m1")).thenReturn(Optional.of(mod));

        var resp = service.effectiveAccess(user);
        assertThat(resp.customers()).hasSize(1);
        assertThat(resp.customers().get(0).enabledModuleCodes()).containsExactly("PRODUCT_COSTING");
        assertThat(resp.customers().get(0).roleCodes()).contains("COSTING_ENGINEER");
    }

    @Test
    void assertModuleEnabled_blocksDisabled() {
        PlatformModule mod = new PlatformModule();
        mod.setModuleId("m1");
        mod.setModuleCode("VENDOR_SELECTION");
        when(moduleRepository.findByModuleCodeIgnoreCase("VENDOR_SELECTION")).thenReturn(Optional.of(mod));
        CustomerModuleEntitlement e = new CustomerModuleEntitlement();
        e.setEnabled(false);
        when(entitlementRepository.findByCustomerIdAndModuleId("c1", "m1")).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> service.assertModuleEnabled("c1", "VENDOR_SELECTION"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void setEntitlement_createsWhenMissing() {
        PlatformCustomer c = new PlatformCustomer();
        c.setCustomerId("c1");
        when(customerRepository.findById("c1")).thenReturn(Optional.of(c));
        PlatformModule m = new PlatformModule();
        m.setModuleId("m1");
        m.setModuleCode("PRODUCT_COSTING");
        when(moduleRepository.findByModuleCodeIgnoreCase("PRODUCT_COSTING")).thenReturn(Optional.of(m));
        when(entitlementRepository.findByCustomerIdAndModuleId("c1", "m1")).thenReturn(Optional.empty());
        when(entitlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.setEntitlement(new SetEntitlementRequest("c1", "PRODUCT_COSTING", true, null, null));
        assertThat(resp.enabled()).isTrue();
        assertThat(resp.moduleCode()).isEqualTo("PRODUCT_COSTING");
    }

    @Test
    void assertUserInCustomer_blocksNonMember() {
        PlatformCustomer c2 = new PlatformCustomer();
        c2.setCustomerId("c2");
        c2.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findById("c2")).thenReturn(Optional.of(c2));
        when(membershipRepository.existsByCustomerIdAndUserIdAndStatus(
                "c2", "u1", MembershipStatus.ACTIVE)).thenReturn(false);
        assertThatThrownBy(() -> service.assertUserInCustomer("c2", "u1"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not an active member");
    }

    @Test
    void customerA_productCostingOnly_vendorSelectionDisabled_403() {
        PlatformModule vs = new PlatformModule();
        vs.setModuleId("m-vs");
        vs.setModuleCode("VENDOR_SELECTION");
        when(moduleRepository.findByModuleCodeIgnoreCase("VENDOR_SELECTION")).thenReturn(Optional.of(vs));

        CustomerModuleEntitlement disabled = new CustomerModuleEntitlement();
        disabled.setEnabled(false);
        when(entitlementRepository.findByCustomerIdAndModuleId("customer-a", "m-vs"))
                .thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.assertModuleEnabled("customer-a", "VENDOR_SELECTION"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("disabled");
        try {
            service.assertModuleEnabled("customer-a", "VENDOR_SELECTION");
        } catch (BusinessRuleViolationException ex) {
            org.assertj.core.api.Assertions.assertThat(ex.getRuleReference()).isEqualTo("ACCESS-03");
        }
    }

    @Test
    void getCustomer_notFound() {
        when(customerRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCustomer("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setCustomerStatus_deactivate() {
        PlatformCustomer c = new PlatformCustomer();
        c.setCustomerId("c1");
        c.setCustomerCode("ACME");
        c.setCustomerName("Acme");
        c.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findById("c1")).thenReturn(Optional.of(c));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.setCustomerStatus("c1", new com.automotive.costing.platform.access.dto.AccessDtos.SetCustomerStatusRequest("INACTIVE"));
        assertThat(resp.status()).isEqualTo("INACTIVE");
    }

    @Test
    void deactivateMembership_setsInactive() {
        CustomerUserMembership m = new CustomerUserMembership();
        m.setMembershipId("mem1");
        m.setCustomerId("c1");
        m.setUserId("u1");
        m.setStatus(MembershipStatus.ACTIVE);
        when(membershipRepository.findById("mem1")).thenReturn(Optional.of(m));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.deactivateMembership("mem1");
        assertThat(resp.status()).isEqualTo("INACTIVE");
    }

    @Test
    void updateCustomer_renames() {
        PlatformCustomer c = new PlatformCustomer();
        c.setCustomerId("c1");
        c.setCustomerCode("ACME");
        c.setCustomerName("Old");
        c.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findById("c1")).thenReturn(Optional.of(c));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.updateCustomer("c1", new com.automotive.costing.platform.access.dto.AccessDtos.UpdateCustomerRequest("New Name"));
        assertThat(resp.customerName()).isEqualTo("New Name");
    }

    @Test
    void productCosting_disabled_ACCESS_03() {
        PlatformModule pc = new PlatformModule();
        pc.setModuleId("m-pc");
        pc.setModuleCode("PRODUCT_COSTING");
        when(moduleRepository.findByModuleCodeIgnoreCase("PRODUCT_COSTING")).thenReturn(Optional.of(pc));

        CustomerModuleEntitlement disabled = new CustomerModuleEntitlement();
        disabled.setEnabled(false);
        when(entitlementRepository.findByCustomerIdAndModuleId("customer-b", "m-pc"))
                .thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.assertModuleEnabled("customer-b", "PRODUCT_COSTING"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("disabled");
        try {
            service.assertModuleEnabled("customer-b", "PRODUCT_COSTING");
        } catch (BusinessRuleViolationException ex) {
            org.assertj.core.api.Assertions.assertThat(ex.getRuleReference()).isEqualTo("ACCESS-03");
        }
    }
}
