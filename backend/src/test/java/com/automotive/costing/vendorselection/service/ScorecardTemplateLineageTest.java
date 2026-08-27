package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.ScorecardScoringEngine;
import com.automotive.costing.vendorselection.entity.ScorecardTemplateStatus;
import com.automotive.costing.vendorselection.entity.VendorScorecardTemplate;
import com.automotive.costing.vendorselection.repository.VendorScorecardCategoryRepository;
import com.automotive.costing.vendorselection.repository.VendorScorecardResultRepository;
import com.automotive.costing.vendorselection.repository.VendorScorecardTemplateRepository;
import com.automotive.costing.vendorselection.service.ScorecardService.CategoryDef;
import com.automotive.costing.vendorselection.service.ScorecardService.CloneTemplateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScorecardTemplateLineageTest {

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
    void clone_setsParentAndIncrementsVersion() {
        VendorScorecardTemplate source = new VendorScorecardTemplate();
        source.setTemplateId("root-1");
        source.setTemplateCode("VS-DEFAULT-100");
        source.setTemplateName("Default");
        source.setStatus(ScorecardTemplateStatus.ACTIVE);
        source.setLineageRootId("root-1");
        source.setVersionNo(1);

        when(templateRepository.findByCustomerIdAndTemplateId("cust-test-1", "root-1")).thenReturn(Optional.of(source));
        when(templateRepository.existsByTemplateCodeIgnoreCase("CUSTOM-A")).thenReturn(false);
        when(templateRepository.countByLineageRootId("root-1")).thenReturn(1);
        // clone may load source categories and toTemplate loads by new template id
        lenient().when(categoryRepository.findByTemplateIdOrderBySequenceNoAsc(anyString())).thenReturn(List.of());
        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var out = service.cloneTemplate("root-1", new CloneTemplateRequest(
                "CUSTOM-A", "Customer A", null,
                List.of(new CategoryDef("QUALITY", "Quality", new BigDecimal("100"), 1))));

        assertThat(out.parentTemplateId()).isEqualTo("root-1");
        assertThat(out.lineageRootId()).isEqualTo("root-1");
        assertThat(out.versionNo()).isEqualTo(2);
        assertThat(out.templateCode()).isEqualTo("CUSTOM-A");
    }
}
