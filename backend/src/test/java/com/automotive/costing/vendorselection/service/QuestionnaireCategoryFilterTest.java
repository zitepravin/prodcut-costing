package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.entity.QuestionCategory;
import com.automotive.costing.vendorselection.entity.VendorQuestion;
import com.automotive.costing.vendorselection.engine.QuestionnaireEvaluationEngine;
import com.automotive.costing.vendorselection.engine.QuestionnaireGateEngine;
import com.automotive.costing.vendorselection.repository.VendorQuestionRepository;
import com.automotive.costing.vendorselection.repository.VendorQuestionnaireEvaluationRepository;
import com.automotive.costing.vendorselection.repository.VendorQuestionnaireResponseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireCategoryFilterTest {

    @Mock private VendorQuestionRepository questionRepository;
    @Mock private VendorQuestionnaireResponseRepository responseRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private QuestionnaireGateEngine gateEngine;
    @Mock private QuestionnaireEvaluationEngine evaluationEngine;
    @Mock private VendorQuestionnaireEvaluationRepository evaluationRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private AuditService auditService;
    private QuestionnaireService service;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireService(questionRepository, responseRepository, tenantAccessGuard,
                supplierMasterService, gateEngine, evaluationEngine, evaluationRepository, objectMapper, auditService);
        when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-1");
    }

    @Test
    void listActiveQuestions_withCategory_usesFilteredQuery() {
        VendorQuestion q = new VendorQuestion();
        q.setQuestionId("q1");
        q.setQuestionCode("VS-Q-AS-DEDICATED");
        q.setCategory(QuestionCategory.SERVICE);
        q.setActiveFlag(true);
        q.setVersionNo(1);
        q.setQuestionText("Dedicated after-sales?");
        q.setAnswerType(com.automotive.costing.vendorselection.entity.QuestionAnswerType.YES_NO);
        when(questionRepository.findByCustomerIdAndCategoryAndActiveFlagTrueOrderByQuestionCodeAsc("cust-1", QuestionCategory.SERVICE))
                .thenReturn(List.of(q));
        var result = service.listActiveQuestions(QuestionCategory.SERVICE);
        assertThat(result).hasSize(1);
        verify(questionRepository).findByCustomerIdAndCategoryAndActiveFlagTrueOrderByQuestionCodeAsc("cust-1", QuestionCategory.SERVICE);
    }

    @Test
    void listActiveQuestions_nullCategory_listsAll() {
        when(questionRepository.findByCustomerIdAndActiveFlagTrueOrderByCategoryAscQuestionCodeAsc("cust-1")).thenReturn(List.of());
        assertThat(service.listActiveQuestions(null)).isEmpty();
    }
}
