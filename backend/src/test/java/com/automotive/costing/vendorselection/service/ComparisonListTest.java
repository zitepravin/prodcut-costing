package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.SupplierComparisonEngine;
import com.automotive.costing.vendorselection.entity.VendorComparison;
import com.automotive.costing.vendorselection.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComparisonListTest {

    @Mock private SupplierMasterService supplierMasterService;
    @Mock private SupplierCapabilityRepository capabilityRepository;
    @Mock private SupplierQualityMetricRepository qualityRepository;
    @Mock private SupplierDeliveryMetricRepository deliveryRepository;
    @Mock private SupplierWarrantyTermRepository warrantyRepository;
    @Mock private SupplierTcoCalculationRepository tcoRepository;
    @Mock private VendorScorecardResultRepository scorecardResultRepository;
    @Mock private VendorComparisonRepository comparisonRepository;
    @Mock private SupplierQualificationCurrentRepository qualificationCurrentRepository;
    @Mock private SupplierComparisonEngine comparisonEngine;
    @Mock private AuditService auditService;
    @Mock private ObjectMapper objectMapper;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private SupplierComparisonService service;

    @Test
    void listRecent_mapsSummaries() {
        VendorComparison c = new VendorComparison();
        c.setComparisonId("cmp-1");
        c.setBomNodeId("bom-1");
        c.setProgramId("prog-1");
        c.setSupplierIdsCsv("S1,S2");
        c.setCreatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        when(comparisonRepository.findTop50ByCustomerIdOrderByCreatedAtDesc("cust-test-1")).thenReturn(List.of(c));

        var list = service.listRecent();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).comparisonId()).isEqualTo("cmp-1");
        assertThat(list.get(0).supplierIdsCsv()).isEqualTo("S1,S2");
    }

    @Test
    void listByBomNode_requiresId() {
        assertThatThrownBy(() -> service.listByBomNode("  "))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
