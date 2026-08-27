package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.EvidenceStatusTransitionEngine;
import com.automotive.costing.vendorselection.engine.WarrantyClaimStatusTransitionEngine;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierWarrantyClaim;
import com.automotive.costing.vendorselection.entity.WarrantyClaimStatus;
import com.automotive.costing.vendorselection.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantyClaimListFilterTest {

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
    @Mock private WarrantyClaimStatusTransitionEngine claimStatusTransitionEngine;
    @Mock private EvidenceStatusTransitionEngine evidenceStatusTransitionEngine;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private SupplierEvidenceService service;

    @Test
    void listWarrantyClaims_bothFilters_usesCombinedQuery() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        SupplierWarrantyClaim c = new SupplierWarrantyClaim();
        c.setClaimId("c1");
        c.setSupplierId("S1");
        c.setClaimDate(LocalDate.of(2026, 1, 1));
        c.setClaimStatus(WarrantyClaimStatus.OPEN);
        c.setEvidenceStatus(EvidenceStatus.PENDING);
        when(warrantyClaimRepository.findBySupplierIdAndClaimStatusAndEvidenceStatusOrderByClaimDateDesc(
                "S1", WarrantyClaimStatus.OPEN, EvidenceStatus.PENDING)).thenReturn(List.of(c));

        var list = service.listWarrantyClaims("S1", WarrantyClaimStatus.OPEN, EvidenceStatus.PENDING);
        assertThat(list).hasSize(1);
        verify(warrantyClaimRepository).findBySupplierIdAndClaimStatusAndEvidenceStatusOrderByClaimDateDesc(
                "S1", WarrantyClaimStatus.OPEN, EvidenceStatus.PENDING);
    }
}
