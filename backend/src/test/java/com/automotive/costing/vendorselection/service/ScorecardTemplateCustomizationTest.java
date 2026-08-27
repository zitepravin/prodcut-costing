package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.ScorecardScoringEngine;
import com.automotive.costing.vendorselection.entity.ScorecardTemplateStatus;
import com.automotive.costing.vendorselection.entity.VendorScorecardTemplate;
import com.automotive.costing.vendorselection.repository.VendorScorecardCategoryRepository;
import com.automotive.costing.vendorselection.repository.VendorScorecardResultRepository;
import com.automotive.costing.vendorselection.repository.VendorScorecardTemplateRepository;
import com.automotive.costing.vendorselection.service.ScorecardService.CategoryDef;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScorecardTemplateCustomizationTest {

    @Mock private VendorScorecardTemplateRepository templateRepository;
    @Mock private VendorScorecardCategoryRepository categoryRepository;
    @Mock private VendorScorecardResultRepository resultRepository;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private ScorecardScoringEngine scoringEngine;
    @Mock private AuditService auditService;
    @Mock private ObjectMapper objectMapper;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private ScorecardService service;

    @Test
    void replaceCategories_whenTemplateUsed_fails() {
        VendorScorecardTemplate t = new VendorScorecardTemplate();
        t.setTemplateId("t1");
        t.setStatus(ScorecardTemplateStatus.ACTIVE);
        when(templateRepository.findByCustomerIdAndTemplateId("cust-test-1", "t1")).thenReturn(Optional.of(t));
        when(resultRepository.countByTemplateId("t1")).thenReturn(3L);

        assertThatThrownBy(() -> service.replaceCategoriesIfUnused("t1", List.of(
                new CategoryDef("QUALITY", "Quality", new BigDecimal("100"), 1))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Clone");
    }
}
