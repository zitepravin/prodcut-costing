package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyPolicyRequest;
import com.automotive.costing.vendorselection.engine.EvidenceStatusTransitionEngine;
import com.automotive.costing.vendorselection.engine.WarrantyClaimStatusTransitionEngine;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierPolicyEvidence;
import com.automotive.costing.vendorselection.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tester scenarios: declaration is not verified evidence.
 */
@ExtendWith(MockitoExtension.class)
class PolicyEvidenceQaScenariosTest {

    @Mock private SupplierQualityMetricRepository qualityRepository;
    @Mock private com.automotive.costing.platform.access.tenant.TenantAccessGuard tenantAccessGuard;
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

    
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private SupplierEvidenceService service;

    @Test
    @DisplayName("QA: Cannot VERIFIED declaration-only policy without evidence reference")
    void verifiedDeclarationWithoutRef_fails() {
        SupplierPolicyEvidence p = new SupplierPolicyEvidence();
        p.setPolicyId("p1");
        p.setSupplierId("S1");
        p.setDeclarationOnly(true);
        p.setEvidenceStatus(EvidenceStatus.PENDING);
        p.setEvidenceRef(null);
        when(policyRepository.findByCustomerIdAndPolicyId("cust-test-1", "p1")).thenReturn(Optional.of(p));

        VerifyPolicyRequest req = new VerifyPolicyRequest(EvidenceStatus.VERIFIED, null, null);
        assertThatThrownBy(() -> service.verifyPolicy("p1", req))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("QA: WAIVED without waiverReason is rejected")
    void waivedWithoutReason_fails() {
        SupplierPolicyEvidence p = new SupplierPolicyEvidence();
        p.setPolicyId("p1");
        p.setSupplierId("S1");
        p.setDeclarationOnly(false);
        p.setEvidenceStatus(EvidenceStatus.PENDING);
        when(policyRepository.findByCustomerIdAndPolicyId("cust-test-1", "p1")).thenReturn(Optional.of(p));

        VerifyPolicyRequest req = new VerifyPolicyRequest(EvidenceStatus.WAIVED, "ref", null);
        assertThatThrownBy(() -> service.verifyPolicy("p1", req))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
