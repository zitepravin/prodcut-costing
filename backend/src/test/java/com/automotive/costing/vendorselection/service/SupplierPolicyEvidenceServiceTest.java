package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreatePolicyEvidenceRequest;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.PolicyEvidenceResponse;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyPolicyRequest;
import com.automotive.costing.vendorselection.repository.*;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.PolicyType;
import com.automotive.costing.vendorselection.entity.SupplierPolicyEvidence;
import com.automotive.costing.vendorselection.repository.SupplierCapabilityRepository;
import com.automotive.costing.vendorselection.repository.SupplierDeliveryMetricRepository;
import com.automotive.costing.vendorselection.repository.SupplierGuaranteeTermRepository;
import com.automotive.costing.vendorselection.repository.SupplierPolicyEvidenceRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualityMetricRepository;
import com.automotive.costing.vendorselection.repository.SupplierWarrantyTermRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.automotive.costing.vendorselection.engine.EvidenceStatusTransitionEngine;
import com.automotive.costing.vendorselection.engine.WarrantyClaimStatusTransitionEngine;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierPolicyEvidenceServiceTest {

    @Mock private SupplierQualityMetricRepository qualityRepository;
    @Mock private SupplierDeliveryMetricRepository deliveryRepository;
    @Mock private SupplierWarrantyTermRepository warrantyRepository;
    @Mock private SupplierWarrantyClaimRepository warrantyClaimRepository;
    @Mock private SupplierGuaranteeTermRepository guaranteeRepository;
    @Mock private SupplierGuaranteeEventRepository guaranteeEventRepository;
    @Mock private SupplierAfterSalesMetricRepository afterSalesRepository;
    @Mock private SupplierPerformanceFeedbackRepository performanceFeedbackRepository;
    @Mock private SupplierCapabilityRepository capabilityRepository;
    @Mock private SupplierPolicyEvidenceRepository policyRepository;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private AuditService auditService;
    @Spy private EvidenceStatusTransitionEngine evidenceStatusTransitionEngine = new EvidenceStatusTransitionEngine();
    @Spy private WarrantyClaimStatusTransitionEngine claimStatusTransitionEngine = new WarrantyClaimStatusTransitionEngine();

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private SupplierEvidenceService service;

    @Test
    void addPolicy_startsPending_notVerified() {
        when(supplierMasterService.require("s1")).thenReturn(new SupplierMaster());
        when(policyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PolicyEvidenceResponse r = service.addPolicy(new CreatePolicyEvidenceRequest(
                "s1",
                PolicyType.QUALITY_POLICY,
                "ISO 9001 Quality Manual",
                "Internal QM",
                "v3.1",
                null, null, null,
                true,
                null,
                null));

        assertThat(r.evidenceStatus()).isEqualTo(EvidenceStatus.PENDING);
        assertThat(r.declarationOnly()).isTrue();
        assertThat(r.policyType()).isEqualTo(PolicyType.QUALITY_POLICY);
        assertThat(r.verifiedBy()).isNull();
    }

    @Test
    void waiveWithoutReason_fails() {
        SupplierPolicyEvidence p = new SupplierPolicyEvidence();
        p.setPolicyId("p1");
        p.setSupplierId("s1");
        p.setEvidenceStatus(EvidenceStatus.PENDING);
        when(policyRepository.findByCustomerIdAndPolicyId("cust-test-1", "p1")).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.verifyPolicy("p1",
                new VerifyPolicyRequest(EvidenceStatus.WAIVED, null, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("waiverReason");
    }

    @Test
    void verifyDeclarationOnlyWithoutEvidenceRef_fails() {
        SupplierPolicyEvidence p = new SupplierPolicyEvidence();
        p.setPolicyId("p1");
        p.setSupplierId("s1");
        p.setDeclarationOnly(true);
        p.setEvidenceStatus(EvidenceStatus.PENDING);
        p.setEvidenceRef(null);
        when(policyRepository.findByCustomerIdAndPolicyId("cust-test-1", "p1")).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.verifyPolicy("p1",
                new VerifyPolicyRequest(EvidenceStatus.VERIFIED, null, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("declaration-only");
    }

    @Test
    void verifyWithEvidenceRef_succeeds() {
        SupplierPolicyEvidence p = new SupplierPolicyEvidence();
        p.setPolicyId("p1");
        p.setSupplierId("s1");
        p.setPolicyType(PolicyType.ENVIRONMENTAL_POLICY);
        p.setPolicyTitle("ISO 14001");
        p.setDeclarationOnly(true);
        p.setEvidenceStatus(EvidenceStatus.PENDING);
        when(policyRepository.findByCustomerIdAndPolicyId("cust-test-1", "p1")).thenReturn(Optional.of(p));
        when(policyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PolicyEvidenceResponse r = service.verifyPolicy("p1",
                new VerifyPolicyRequest(EvidenceStatus.VERIFIED, "DOC-ENV-001", null));

        assertThat(r.evidenceStatus()).isEqualTo(EvidenceStatus.VERIFIED);
        assertThat(r.evidenceRef()).isEqualTo("DOC-ENV-001");
    }

    @Test
    void policySummary_countsByStatus() {
        when(supplierMasterService.require("s1")).thenReturn(new SupplierMaster());

        SupplierPolicyEvidence a = base("a", EvidenceStatus.VERIFIED, false);
        SupplierPolicyEvidence b = base("b", EvidenceStatus.PENDING, true);
        SupplierPolicyEvidence c = base("c", EvidenceStatus.FAILED, false);
        SupplierPolicyEvidence d = base("d", EvidenceStatus.WAIVED, false);
        when(policyRepository.findByCustomerIdAndSupplierIdOrderByCreatedAtDesc("cust-test-1", "s1"))
                .thenReturn(List.of(a, b, c, d));

        var summary = service.policySummary("s1");
        assertThat(summary.totalCount()).isEqualTo(4);
        assertThat(summary.verifiedCount()).isEqualTo(1);
        assertThat(summary.pendingCount()).isEqualTo(1);
        assertThat(summary.failedOrExpiredCount()).isEqualTo(1);
        assertThat(summary.waivedCount()).isEqualTo(1);
        assertThat(summary.declarationOnlyCount()).isEqualTo(1);
    }

    private static SupplierPolicyEvidence base(String id, EvidenceStatus status, boolean declarationOnly) {
        SupplierPolicyEvidence p = new SupplierPolicyEvidence();
        p.setPolicyId(id);
        p.setSupplierId("s1");
        p.setPolicyType(PolicyType.OTHER);
        p.setPolicyTitle("t-" + id);
        p.setEvidenceStatus(status);
        p.setDeclarationOnly(declarationOnly);
        return p;
    }
}
