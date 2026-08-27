package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.ScorecardScoringEngine;
import com.automotive.costing.vendorselection.entity.VendorScorecardResult;
import com.automotive.costing.vendorselection.repository.VendorScorecardCategoryRepository;
import com.automotive.costing.vendorselection.repository.VendorScorecardResultRepository;
import com.automotive.costing.vendorselection.repository.VendorScorecardTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScorecardHistoryAndExpiryTest {

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
@InjectMocks private ScorecardService scorecardService;

    @Test
    void listResultsBySupplier_mapsHistory() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        VendorScorecardResult r = new VendorScorecardResult();
        r.setResultId("res1");
        r.setTemplateId("t1");
        r.setSupplierId("S1");
        r.setOverallScore(new BigDecimal("88.5"));
        r.setMaxScore(new BigDecimal("100"));
        r.setScoredAt(Instant.parse("2026-01-15T10:00:00Z"));
        when(resultRepository.findByCustomerIdAndSupplierIdOrderByScoredAtDesc("cust-test-1", "S1")).thenReturn(List.of(r));

        var list = scorecardService.listResultsBySupplier("S1");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).overallScore()).isEqualByComparingTo("88.5");
        assertThat(list.get(0).resultId()).isEqualTo("res1");
    }
}
