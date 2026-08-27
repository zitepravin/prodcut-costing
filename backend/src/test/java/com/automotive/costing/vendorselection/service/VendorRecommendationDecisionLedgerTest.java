package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.engine.RecommendationExplainabilityEngine;
import com.automotive.costing.vendorselection.engine.RecommendationRankingEngine;
import com.automotive.costing.vendorselection.entity.RecommendationStatus;
import com.automotive.costing.vendorselection.entity.VendorRecommendation;
import com.automotive.costing.vendorselection.repository.VendorRecommendationCandidateRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationDecisionRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationDecisionEventRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.automotive.costing.vendorselection.repository.VendorRecommendationExplanationRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import com.automotive.costing.vendorselection.service.VendorRecommendationService.DecisionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorRecommendationDecisionLedgerTest {

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

    @Test
    void overrideWithoutReason_fails() {
        VendorRecommendation rec = new VendorRecommendation();
        rec.setRecommendationId("r1");
        rec.setStatus(RecommendationStatus.SUBMITTED);
        when(recommendationRepository.findByCustomerIdAndRecommendationId("cust-test-1", "r1")).thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> service.override("r1",
                new DecisionRequest("comment", "S-ALT", null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void approve_writesDecisionLedger() {
        VendorRecommendation rec = new VendorRecommendation();
        rec.setRecommendationId("r1");
        rec.setStatus(RecommendationStatus.SUBMITTED);
        rec.setRecommendedSupplierId("S1");
        when(recommendationRepository.findByCustomerIdAndRecommendationId("cust-test-1", "r1")).thenReturn(Optional.of(rec));
        when(recommendationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(explanationRepository.findByRecommendationId("r1")).thenReturn(Optional.empty());
        when(decisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(decisionEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        } catch (Exception ignored) {}

        service.approve("r1", new DecisionRequest("ok", null, null));

        ArgumentCaptor<com.automotive.costing.vendorselection.entity.VendorRecommendationDecision> cap =
                ArgumentCaptor.forClass(com.automotive.costing.vendorselection.entity.VendorRecommendationDecision.class);
        verify(decisionRepository).save(cap.capture());
        assertThat(cap.getValue().getDecisionAction().name()).isEqualTo("APPROVE");
        assertThat(cap.getValue().getPreviousStatus()).isEqualTo(RecommendationStatus.SUBMITTED);
        assertThat(cap.getValue().getNewStatus()).isEqualTo(RecommendationStatus.APPROVED);
        assertThat(cap.getValue().getRecommendedSupplierId()).isEqualTo("S1");
    }
}
