package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.EvidenceStatusTransitionEngine;
import com.automotive.costing.vendorselection.engine.WarrantyClaimStatusTransitionEngine;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.GuaranteeEventType;
import com.automotive.costing.vendorselection.entity.SupplierGuaranteeEvent;
import com.automotive.costing.vendorselection.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuaranteeEventQaScenariosTest {

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
    @Spy private WarrantyClaimStatusTransitionEngine claimStatusTransitionEngine = new WarrantyClaimStatusTransitionEngine();
    @Spy private EvidenceStatusTransitionEngine evidenceStatusTransitionEngine = new EvidenceStatusTransitionEngine();

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private SupplierEvidenceService service;

    private SupplierGuaranteeEvent event(boolean resolved, EvidenceStatus es) {
        SupplierGuaranteeEvent e = new SupplierGuaranteeEvent();
        e.setEventId("e1");
        e.setSupplierId("S1");
        e.setEventDate(LocalDate.of(2026, 4, 1));
        e.setEventType(GuaranteeEventType.FAILURE);
        e.setResolvedFlag(resolved);
        e.setEvidenceStatus(es);
        return e;
    }

    @Test
    @DisplayName("QA: unresolved + PENDING uses combined repository method")
    void unresolvedAndPending_andFilter() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        when(guaranteeEventRepository.findBySupplierIdAndResolvedFlagAndEvidenceStatusOrderByEventDateDesc(
                "S1", false, EvidenceStatus.PENDING)).thenReturn(List.of(event(false, EvidenceStatus.PENDING)));

        var list = service.listGuaranteeEvents("S1", true, EvidenceStatus.PENDING);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).resolvedFlag()).isFalse();
        verify(guaranteeEventRepository).findBySupplierIdAndResolvedFlagAndEvidenceStatusOrderByEventDateDesc(
                "S1", false, EvidenceStatus.PENDING);
    }

    @Test
    @DisplayName("QA: resolved only filter")
    void resolvedOnly() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        when(guaranteeEventRepository.findBySupplierIdAndResolvedFlagOrderByEventDateDesc("S1", true))
                .thenReturn(List.of(event(true, EvidenceStatus.VERIFIED)));

        var list = service.listGuaranteeEvents("S1", false, null);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).resolvedFlag()).isTrue();
    }
}
