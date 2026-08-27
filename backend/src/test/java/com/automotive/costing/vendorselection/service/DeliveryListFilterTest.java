package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierDeliveryMetric;
import com.automotive.costing.vendorselection.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryListFilterTest {

    @Mock private SupplierQualityMetricRepository qualityRepository;
    @Mock private SupplierDeliveryMetricRepository deliveryRepository;
    @Mock private SupplierWarrantyTermRepository warrantyRepository;
    @Mock private SupplierWarrantyClaimRepository warrantyClaimRepository;
    @Mock private SupplierGuaranteeTermRepository guaranteeRepository;
    @Mock private SupplierGuaranteeEventRepository guaranteeEventRepository;
    @Mock private SupplierCapabilityRepository capabilityRepository;
    @Mock private SupplierAfterSalesMetricRepository afterSalesRepository;
    @Mock private SupplierPerformanceFeedbackRepository performanceFeedbackRepository;
    @Mock private SupplierPolicyEvidenceRepository policyRepository;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private AuditService auditService;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private SupplierEvidenceService service;

    @Test
    void listDelivery_withEvidenceStatus_usesFilteredQuery() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        SupplierDeliveryMetric m = new SupplierDeliveryMetric();
        m.setMetricId("d1");
        m.setSupplierId("S1");
        m.setPeriodStart(LocalDate.of(2026, 1, 1));
        m.setPeriodEnd(LocalDate.of(2026, 3, 31));
        m.setOtdPercent(new BigDecimal("96.5"));
        m.setEvidenceStatus(EvidenceStatus.PENDING);
        when(deliveryRepository.findByCustomerIdAndSupplierIdAndEvidenceStatusOrderByPeriodEndDesc("cust-test-1", 
                "S1", EvidenceStatus.PENDING)).thenReturn(List.of(m));

        var list = service.listDelivery("S1", EvidenceStatus.PENDING);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).otdPercent()).isEqualByComparingTo("96.5");
        verify(deliveryRepository).findByCustomerIdAndSupplierIdAndEvidenceStatusOrderByPeriodEndDesc("cust-test-1", 
                "S1", EvidenceStatus.PENDING);
    }
}
