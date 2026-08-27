package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.PolicyType;
import com.automotive.costing.vendorselection.entity.SupplierPolicyEvidence;
import com.automotive.costing.vendorselection.repository.*;
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
class PolicyListFilterTest {

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
    void listPolicies_withEvidenceStatus_usesFilteredQuery() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        SupplierPolicyEvidence p = new SupplierPolicyEvidence();
        p.setPolicyId("pol1");
        p.setSupplierId("S1");
        p.setPolicyType(PolicyType.QUALITY_POLICY);
        p.setPolicyTitle("QMS");
        p.setEvidenceStatus(EvidenceStatus.PENDING);
        when(policyRepository.findByCustomerIdAndSupplierIdAndEvidenceStatus("cust-test-1", "S1", EvidenceStatus.PENDING))
                .thenReturn(List.of(p));

        var list = service.listPolicies("S1", EvidenceStatus.PENDING, null);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).policyType()).isEqualTo(PolicyType.QUALITY_POLICY);
        verify(policyRepository).findByCustomerIdAndSupplierIdAndEvidenceStatus("cust-test-1", "S1", EvidenceStatus.PENDING);
    }
}
