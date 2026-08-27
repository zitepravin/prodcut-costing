package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.entity.*;
import com.automotive.costing.vendorselection.repository.SupplierRiskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListFilterServiceTest {

    @Mock private SupplierRiskRepository riskRepository;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private AuditService auditService;
    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private SupplierRiskService riskService;

    @Test
    void listBySupplier_withStatusAndSeverity_usesFilteredQuery() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        SupplierRisk r = new SupplierRisk();
        r.setRiskId("r1");
        r.setSupplierId("S1");
        r.setRiskCategory(RiskCategory.QUALITY);
        r.setRiskTitle("PPM");
        r.setSeverity(RiskSeverity.HIGH);
        r.setLikelihood(RiskLikelihood.POSSIBLE);
        r.setStatus(RiskStatus.OPEN);
        when(riskRepository.findByCustomerIdAndSupplierIdAndStatusAndSeverityOrderByIdentifiedAtDesc("cust-test-1", 
                "S1", RiskStatus.OPEN, RiskSeverity.HIGH)).thenReturn(List.of(r));

        var list = riskService.listBySupplier("S1", RiskStatus.OPEN, RiskSeverity.HIGH);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).severity()).isEqualTo(RiskSeverity.HIGH);
        verify(riskRepository).findByCustomerIdAndSupplierIdAndStatusAndSeverityOrderByIdentifiedAtDesc("cust-test-1", 
                "S1", RiskStatus.OPEN, RiskSeverity.HIGH);
    }
}
