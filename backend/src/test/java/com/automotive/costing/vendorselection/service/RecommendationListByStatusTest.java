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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationListByStatusTest {

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
    void listByStatus_returnsMappedRows() {
        VendorRecommendation r = new VendorRecommendation();
        r.setRecommendationId("rec-1");
        r.setSourcingEventCode("SE-1");
        r.setStatus(RecommendationStatus.SUBMITTED);
        when(recommendationRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc("cust-test-1", RecommendationStatus.SUBMITTED))
                .thenReturn(List.of(r));
        when(candidateRepository.findByRecommendationIdOrderByRankPositionAsc("rec-1"))
                .thenReturn(List.of());

        var list = service.listByStatus(RecommendationStatus.SUBMITTED);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).recommendationId()).isEqualTo("rec-1");
        assertThat(list.get(0).status()).isEqualTo("SUBMITTED");
    }

    @Test
    void listByStatus_null_throws() {
        assertThatThrownBy(() -> service.listByStatus(null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
