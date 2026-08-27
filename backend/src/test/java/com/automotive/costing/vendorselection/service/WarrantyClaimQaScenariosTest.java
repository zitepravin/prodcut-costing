package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.UpdateWarrantyClaimStatusRequest;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyEvidenceRequest;
import com.automotive.costing.vendorselection.engine.EvidenceStatusTransitionEngine;
import com.automotive.costing.vendorselection.engine.WarrantyClaimStatusTransitionEngine;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierWarrantyClaim;
import com.automotive.costing.vendorselection.entity.WarrantyClaimStatus;
import com.automotive.costing.vendorselection.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QA / tester scenarios: warranty claim commercial status + evidence independence.
 */
@ExtendWith(MockitoExtension.class)
class WarrantyClaimQaScenariosTest {

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

    private SupplierWarrantyClaim openPendingClaim() {
        SupplierWarrantyClaim c = new SupplierWarrantyClaim();
        c.setClaimId("claim-1");
        c.setSupplierId("S1");
        c.setClaimDate(LocalDate.of(2026, 3, 1));
        c.setClaimCost(new BigDecimal("100.00"));
        c.setCurrencyCode("INR");
        c.setClaimStatus(WarrantyClaimStatus.OPEN);
        c.setEvidenceStatus(EvidenceStatus.PENDING);
        return c;
    }

    @Test
    @DisplayName("QA: Accept open claim sets ACCEPTED and audits TRANSITION")
    void acceptOpenClaim() {
        SupplierWarrantyClaim c = openPendingClaim();
        when(warrantyClaimRepository.findByCustomerIdAndClaimId("cust-test-1", "claim-1")).thenReturn(Optional.of(c));
        when(warrantyClaimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.updateWarrantyClaimStatus("claim-1",
                new UpdateWarrantyClaimStatusRequest(WarrantyClaimStatus.ACCEPTED, null, null, null, null));

        assertThat(resp.claimStatus()).isEqualTo(WarrantyClaimStatus.ACCEPTED);
        assertThat(resp.acceptedFlag()).isTrue();
        assertThat(resp.evidenceStatus()).isEqualTo(EvidenceStatus.PENDING); // evidence independent
        verify(auditService).recordTransition(eq("SupplierWarrantyClaim"), eq("claim-1"), eq("OPEN"), eq("ACCEPTED"));
    }

    @Test
    @DisplayName("QA: Cannot reopen CLOSED claim")
    void closedIsTerminal() {
        SupplierWarrantyClaim c = openPendingClaim();
        c.setClaimStatus(WarrantyClaimStatus.CLOSED);
        when(warrantyClaimRepository.findByCustomerIdAndClaimId("cust-test-1", "claim-1")).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.updateWarrantyClaimStatus("claim-1",
                new UpdateWarrantyClaimStatusRequest(WarrantyClaimStatus.OPEN, null, null, null, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("QA: Verify evidence does not change claimStatus")
    void verifyEvidenceKeepsClaimStatus() {
        SupplierWarrantyClaim c = openPendingClaim();
        when(warrantyClaimRepository.findByCustomerIdAndClaimId("cust-test-1", "claim-1")).thenReturn(Optional.of(c));
        when(warrantyClaimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.verifyWarrantyClaim("claim-1",
                new VerifyEvidenceRequest(EvidenceStatus.VERIFIED, "ref-9"));

        assertThat(resp.claimStatus()).isEqualTo(WarrantyClaimStatus.OPEN);
        assertThat(resp.evidenceStatus()).isEqualTo(EvidenceStatus.VERIFIED);
        verify(auditService).recordVerify(eq("SupplierWarrantyClaim"), eq("claim-1"), eq("PENDING"), eq("VERIFIED"));
    }

    @Test
    @DisplayName("QA: Combined OPEN+PENDING filter uses AND query")
    void combinedListFilter() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        when(warrantyClaimRepository.findBySupplierIdAndClaimStatusAndEvidenceStatusOrderByClaimDateDesc(
                "S1", WarrantyClaimStatus.OPEN, EvidenceStatus.PENDING))
                .thenReturn(List.of(openPendingClaim()));

        var list = service.listWarrantyClaims("S1", WarrantyClaimStatus.OPEN, EvidenceStatus.PENDING);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).claimStatus()).isEqualTo(WarrantyClaimStatus.OPEN);
    }

    @Test
    @DisplayName("QA: Summary counts closed and withdrawn buckets")
    void summaryBuckets() {
        when(supplierMasterService.require("S1")).thenReturn(null);
        SupplierWarrantyClaim open = openPendingClaim();
        SupplierWarrantyClaim closed = openPendingClaim();
        closed.setClaimId("c2");
        closed.setClaimStatus(WarrantyClaimStatus.CLOSED);
        SupplierWarrantyClaim withdrawn = openPendingClaim();
        withdrawn.setClaimId("c3");
        withdrawn.setClaimStatus(WarrantyClaimStatus.WITHDRAWN);
        when(warrantyClaimRepository.findByCustomerIdAndSupplierIdOrderByClaimDateDesc("cust-test-1", "S1"))
                .thenReturn(List.of(open, closed, withdrawn));

        var summary = service.warrantyClaimSummary("S1");
        assertThat(summary.totalClaims()).isEqualTo(3);
        assertThat(summary.openClaims()).isEqualTo(1);
        assertThat(summary.closedClaims()).isEqualTo(1);
        assertThat(summary.withdrawnClaims()).isEqualTo(1);
    }
}
