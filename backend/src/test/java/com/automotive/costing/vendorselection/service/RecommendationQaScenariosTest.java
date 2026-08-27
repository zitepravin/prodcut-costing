package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.engine.RecommendationExplainabilityEngine;
import com.automotive.costing.vendorselection.engine.RecommendationRankingEngine;
import com.automotive.costing.vendorselection.entity.RecommendationStatus;
import com.automotive.costing.vendorselection.entity.VendorRecommendation;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationCandidateRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationDecisionEventRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationDecisionRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationExplanationRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tester scenarios: human decision queue — system never auto-approves.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationQaScenariosTest {

    @Mock private VendorRecommendationRepository recommendationRepository;
    @Mock private VendorRecommendationCandidateRepository candidateRepository;
    @Mock private SupplierQualificationCurrentRepository qualificationCurrentRepository;
    @Mock private RecommendationRankingEngine rankingEngine;
    @Mock private RecommendationExplainabilityEngine explainabilityEngine;
    @Mock private VendorRecommendationExplanationRepository explanationRepository;
    @Mock private VendorRecommendationDecisionRepository decisionRepository;
    @Mock private VendorRecommendationDecisionEventRepository decisionEventRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;
    @Mock private ObjectMapper objectMapper;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private VendorRecommendationService service;

    private VendorRecommendation rec(String id, RecommendationStatus st) {
        VendorRecommendation r = new VendorRecommendation();
        r.setRecommendationId(id);
        r.setStatus(st);
        r.setRecommendedSupplierId("S1");
        return r;
    }

    @Test
    @DisplayName("QA: Approve from DRAFT is rejected (must be SUBMITTED)")
    void approveFromDraft_fails() {
        when(recommendationRepository.findByCustomerIdAndRecommendationId("cust-test-1", "r1")).thenReturn(Optional.of(rec("r1", RecommendationStatus.DRAFT)));
        assertThatThrownBy(() -> service.approve("r1",
                new VendorRecommendationService.DecisionRequest("ok", null, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("QA: Reject from APPROVED is rejected")
    void rejectFromApproved_fails() {
        when(recommendationRepository.findByCustomerIdAndRecommendationId("cust-test-1", "r1")).thenReturn(Optional.of(rec("r1", RecommendationStatus.APPROVED)));
        assertThatThrownBy(() -> service.reject("r1",
                new VendorRecommendationService.DecisionRequest("no", null, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("QA: Override without overrideReason is rejected")
    void overrideWithoutReason_fails() {
        when(recommendationRepository.findByCustomerIdAndRecommendationId("cust-test-1", "r1")).thenReturn(Optional.of(rec("r1", RecommendationStatus.SUBMITTED)));
        assertThatThrownBy(() -> service.override("r1",
                new VendorRecommendationService.DecisionRequest(null, "S2", null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("QA: Override without overrideSupplierId is rejected")
    void overrideWithoutSupplier_fails() {
        when(recommendationRepository.findByCustomerIdAndRecommendationId("cust-test-1", "r1")).thenReturn(Optional.of(rec("r1", RecommendationStatus.SUBMITTED)));
        assertThatThrownBy(() -> service.override("r1",
                new VendorRecommendationService.DecisionRequest(null, null, "need alternate")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("QA: Approve already APPROVED is rejected")
    void approveAlreadyApproved_fails() {
        when(recommendationRepository.findByCustomerIdAndRecommendationId("cust-test-1", "r1")).thenReturn(Optional.of(rec("r1", RecommendationStatus.APPROVED)));
        assertThatThrownBy(() -> service.approve("r1",
                new VendorRecommendationService.DecisionRequest("again", null, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
