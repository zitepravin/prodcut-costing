package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.entity.*;
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
class EvidenceSummaryServiceTest {

    @Mock private SupplierMasterService supplierMasterService;
    @Mock private SupplierCapabilityRepository capabilityRepository;
    @Mock private SupplierQualityMetricRepository qualityRepository;
    @Mock private SupplierDeliveryMetricRepository deliveryRepository;
    @Mock private SupplierWarrantyTermRepository warrantyRepository;
    @Mock private SupplierWarrantyClaimRepository warrantyClaimRepository;
    @Mock private SupplierGuaranteeTermRepository guaranteeRepository;
    @Mock private SupplierGuaranteeEventRepository guaranteeEventRepository;
    @Mock private SupplierAfterSalesMetricRepository afterSalesRepository;
    @Mock private SupplierPerformanceFeedbackRepository performanceFeedbackRepository;
    @Mock private SupplierPolicyEvidenceRepository policyRepository;
    @Mock private AuditService auditService;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private SupplierEvidenceService service;

    @Test
    void afterSalesSummary_countsVerifiedAndPending() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        SupplierAfterSalesMetric m1 = new SupplierAfterSalesMetric();
        m1.setMetricId("m1");
        m1.setSupplierId("S1");
        m1.setPeriodStart(LocalDate.of(2026, 1, 1));
        m1.setPeriodEnd(LocalDate.of(2026, 3, 31));
        m1.setEvidenceStatus(EvidenceStatus.VERIFIED);
        m1.setServiceSlaMetPercent(new BigDecimal("95"));
        m1.setUnresolvedComplaints(1);
        SupplierAfterSalesMetric m2 = new SupplierAfterSalesMetric();
        m2.setMetricId("m2");
        m2.setSupplierId("S1");
        m2.setPeriodStart(LocalDate.of(2025, 10, 1));
        m2.setPeriodEnd(LocalDate.of(2025, 12, 31));
        m2.setEvidenceStatus(EvidenceStatus.PENDING);
        when(afterSalesRepository.findByCustomerIdAndSupplierIdOrderByPeriodEndDesc("cust-test-1", "S1")).thenReturn(List.of(m1, m2));

        var s = service.afterSalesSummary("S1");
        assertThat(s.totalMetrics()).isEqualTo(2);
        assertThat(s.verifiedMetrics()).isEqualTo(1);
        assertThat(s.pendingMetrics()).isEqualTo(1);
        assertThat(s.latestSlaMetPercent()).isEqualByComparingTo("95");
        assertThat(s.latestUnresolvedComplaints()).isEqualTo(1);
        verify(supplierMasterService).require("S1");
    }

    @Test
    void guaranteeEventSummary_sumsPenaltiesAndUnresolved() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        SupplierGuaranteeEvent e1 = new SupplierGuaranteeEvent();
        e1.setEventId("e1");
        e1.setSupplierId("S1");
        e1.setEventDate(LocalDate.of(2026, 2, 1));
        e1.setEventType(GuaranteeEventType.FAILURE);
        e1.setResolvedFlag(false);
        e1.setEvidenceStatus(EvidenceStatus.VERIFIED);
        e1.setPenaltyAmount(new BigDecimal("1000.00"));
        e1.setCurrencyCode("INR");
        SupplierGuaranteeEvent e2 = new SupplierGuaranteeEvent();
        e2.setEventId("e2");
        e2.setSupplierId("S1");
        e2.setEventDate(LocalDate.of(2026, 1, 1));
        e2.setEventType(GuaranteeEventType.BREACH);
        e2.setResolvedFlag(true);
        e2.setEvidenceStatus(EvidenceStatus.PENDING);
        e2.setPenaltyAmount(new BigDecimal("250.50"));
        e2.setCurrencyCode("INR");
        when(guaranteeEventRepository.findByCustomerIdAndSupplierIdOrderByEventDateDesc("cust-test-1", "S1")).thenReturn(List.of(e1, e2));

        var s = service.guaranteeEventSummary("S1");
        assertThat(s.totalEvents()).isEqualTo(2);
        assertThat(s.unresolvedEvents()).isEqualTo(1);
        assertThat(s.verifiedEvents()).isEqualTo(1);
        assertThat(s.totalPenaltyAmount()).isEqualByComparingTo("1250.50");
        assertThat(s.currencyCode()).isEqualTo("INR");
    }
}
