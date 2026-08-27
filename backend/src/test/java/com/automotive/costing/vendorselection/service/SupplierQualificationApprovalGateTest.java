package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.SupplierLifecycleTransitionEngine;
import com.automotive.costing.vendorselection.entity.RecommendationStatus;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.automotive.costing.vendorselection.entity.VendorRecommendation;
import com.automotive.costing.vendorselection.repository.SupplierQualificationApprovalCheckRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualificationLifecycleEventRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualificationRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationRepository;
import com.automotive.costing.vendorselection.service.SupplierQualificationService.TransitionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierQualificationApprovalGateTest {

    @Mock private SupplierQualificationRepository qualificationRepository;
    @Mock private SupplierQualificationCurrentRepository currentRepository;
    @Mock private SupplierQualificationApprovalCheckRepository approvalCheckRepository;
    @Mock private SupplierQualificationLifecycleEventRepository lifecycleEventRepository;
    @Mock private VendorRecommendationRepository recommendationRepository;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private SupplierLifecycleTransitionEngine transitionEngine;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ObjectMapper objectMapper;
    @Mock private AuditService auditService;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private SupplierQualificationService service;

    @Test
    void approvedWithoutRecommendationId_fails() {
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("S1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.APPROVAL_PENDING);
        when(currentRepository.findById("S1")).thenReturn(Optional.of(cur));

        assertThatThrownBy(() -> service.transition(new TransitionRequest(
                "S1", SupplierLifecycleStatus.APPROVED, "ok", null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void approvedWithDraftRecommendation_fails() {
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("S1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.APPROVAL_PENDING);
        when(currentRepository.findById("S1")).thenReturn(Optional.of(cur));
        VendorRecommendation rec = new VendorRecommendation();
        rec.setRecommendationId("R1");
        rec.setStatus(RecommendationStatus.SUBMITTED);
        rec.setRecommendedSupplierId("S1");
        when(recommendationRepository.findById("R1")).thenReturn(Optional.of(rec));
        when(approvalCheckRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.transition(new TransitionRequest(
                "S1", SupplierLifecycleStatus.APPROVED, "ok", "R1")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void approvedWithWrongSupplierOnRecommendation_fails() {
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("S1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.APPROVAL_PENDING);
        when(currentRepository.findById("S1")).thenReturn(Optional.of(cur));
        VendorRecommendation rec = new VendorRecommendation();
        rec.setRecommendationId("R1");
        rec.setStatus(RecommendationStatus.APPROVED);
        rec.setRecommendedSupplierId("OTHER");
        when(recommendationRepository.findById("R1")).thenReturn(Optional.of(rec));
        when(approvalCheckRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.transition(new TransitionRequest(
                "S1", SupplierLifecycleStatus.APPROVED, "ok", "R1")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
