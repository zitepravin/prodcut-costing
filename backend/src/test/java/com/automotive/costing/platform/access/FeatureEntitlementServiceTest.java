package com.automotive.costing.platform.access;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.entity.CustomerFeatureEntitlement;
import com.automotive.costing.platform.access.entity.CustomerModuleEntitlement;
import com.automotive.costing.platform.access.entity.FeatureCatalog;
import com.automotive.costing.platform.access.entity.PlatformModule;
import com.automotive.costing.platform.access.repository.CustomerFeatureEntitlementRepository;
import com.automotive.costing.platform.access.repository.CustomerModuleEntitlementRepository;
import com.automotive.costing.platform.access.repository.FeatureCatalogRepository;
import com.automotive.costing.platform.access.repository.PlatformModuleRepository;
import com.automotive.costing.platform.access.service.CentralAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeatureEntitlementServiceTest {

    @Mock PlatformModuleRepository moduleRepository;
    @Mock CustomerModuleEntitlementRepository entitlementRepository;
    @Mock FeatureCatalogRepository featureCatalogRepository;
    @Mock CustomerFeatureEntitlementRepository featureEntitlementRepository;

    @InjectMocks CentralAccessService service;

    private static final String CUST = "cust-a";
    private static final String MODULE = "PRODUCT_COSTING";
    private static final String FEAT = "SHOULD_COST_CALCULATE";

    private PlatformModule mod;
    private CustomerModuleEntitlement modEnt;
    private FeatureCatalog feat;
    private CustomerFeatureEntitlement featEnt;

    @BeforeEach
    void setUp() {
        mod = new PlatformModule();
        mod.setModuleId("mod-1");
        mod.setModuleCode(MODULE);
        modEnt = new CustomerModuleEntitlement();
        modEnt.setEntitlementId("me-1");
        modEnt.setCustomerId(CUST);
        modEnt.setModuleId("mod-1");
        modEnt.setEnabled(true);
        feat = new FeatureCatalog();
        feat.setFeatureId("feat-1");
        feat.setFeatureCode(FEAT);
        feat.setFeatureName("Should-Cost Calculate");
        feat.setModuleCode(MODULE);
        feat.setActive(true);
        featEnt = new CustomerFeatureEntitlement();
        featEnt.setEntitlementId("fe-1");
        featEnt.setCustomerId(CUST);
        featEnt.setFeatureId("feat-1");
        featEnt.setEnabled(true);
    }

    private void stubModuleEnabled() {
        when(moduleRepository.findByModuleCodeIgnoreCase(MODULE)).thenReturn(Optional.of(mod));
        when(entitlementRepository.findByCustomerIdAndModuleId(CUST, "mod-1")).thenReturn(Optional.of(modEnt));
    }

    private void stubFeatureCatalog() {
        when(featureCatalogRepository.findByFeatureCodeIgnoreCase(FEAT)).thenReturn(Optional.of(feat));
    }

    private void stubFeatureEntitled() {
        when(featureEntitlementRepository.findByCustomerIdAndFeatureId(CUST, "feat-1"))
                .thenReturn(Optional.of(featEnt));
    }

    @Test @DisplayName("1. feature enabled ALLOW")
    void featureEnabled_allows() {
        stubModuleEnabled(); stubFeatureCatalog(); stubFeatureEntitled();
        service.assertFeatureEnabled(CUST, FEAT);
        assertThat(service.isFeatureEnabled(CUST, FEAT)).isTrue();
    }

    @Test @DisplayName("2. feature disabled DENY")
    void featureDisabled_denies() {
        stubModuleEnabled(); stubFeatureCatalog();
        featEnt.setEnabled(false); stubFeatureEntitled();
        assertThatThrownBy(() -> service.assertFeatureEnabled(CUST, FEAT))
                .isInstanceOf(BusinessRuleViolationException.class).hasMessageContaining("disabled");
    }

    @Test @DisplayName("4. module disabled DENY even if feature on")
    void moduleOff_denies() {
        when(moduleRepository.findByModuleCodeIgnoreCase(MODULE)).thenReturn(Optional.of(mod));
        modEnt.setEnabled(false);
        when(entitlementRepository.findByCustomerIdAndModuleId(CUST, "mod-1")).thenReturn(Optional.of(modEnt));
        stubFeatureCatalog(); stubFeatureEntitled();
        assertThatThrownBy(() -> service.assertFeatureEnabled(CUST, FEAT))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test @DisplayName("wrong customer DENY")
    void wrongCustomer_denies() {
        when(moduleRepository.findByModuleCodeIgnoreCase(MODULE)).thenReturn(Optional.of(mod));
        when(entitlementRepository.findByCustomerIdAndModuleId("other-cust", "mod-1")).thenReturn(Optional.empty());
        stubFeatureCatalog();
        assertThatThrownBy(() -> service.assertFeatureEnabled("other-cust", FEAT))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test @DisplayName("unknown feature DENY")
    void unknownFeature_denies() {
        when(featureCatalogRepository.findByFeatureCodeIgnoreCase("NO_SUCH")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.assertFeatureEnabled(CUST, "NO_SUCH"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("inactive catalog DENY")
    void inactiveCatalog_denies() {
        feat.setActive(false); stubFeatureCatalog();
        assertThatThrownBy(() -> service.assertFeatureEnabled(CUST, FEAT))
                .isInstanceOf(BusinessRuleViolationException.class).hasMessageContaining("not active");
    }

    @Test @DisplayName("missing feature entitlement DENY")
    void missingEntitlement_denies() {
        stubModuleEnabled(); stubFeatureCatalog();
        when(featureEntitlementRepository.findByCustomerIdAndFeatureId(CUST, "feat-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.assertFeatureEnabled(CUST, FEAT))
                .isInstanceOf(BusinessRuleViolationException.class).hasMessageContaining("not entitled");
    }

    @Test @DisplayName("expired feature DENY")
    void expired_denies() {
        stubModuleEnabled(); stubFeatureCatalog();
        featEnt.setEffectiveTo(LocalDate.of(2020, 1, 1)); stubFeatureEntitled();
        assertThatThrownBy(() -> service.assertFeatureEnabled(CUST, FEAT))
                .isInstanceOf(BusinessRuleViolationException.class).hasMessageContaining("expired");
    }
}
