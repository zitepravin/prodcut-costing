package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.QuestionAnswerType;
import com.automotive.costing.vendorselection.entity.QuestionCategory;
import com.automotive.costing.vendorselection.entity.VendorQuestion;
import com.automotive.costing.vendorselection.entity.VendorQuestionnaireResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionnaireEvaluationEngineEvidenceTest {

    private final QuestionnaireEvaluationEngine engine = new QuestionnaireEvaluationEngine();

    private static VendorQuestion gateQuestion() {
        VendorQuestion q = new VendorQuestion();
        q.setQuestionCode("VS-Q-IATF");
        q.setVersionNo(1);
        q.setCategory(QuestionCategory.COMPLIANCE);
        q.setAnswerType(QuestionAnswerType.YES_NO);
        q.setRequiredFlag(true);
        q.setEvidenceRequired(true);
        q.setMandatoryGate(true);
        q.setWeight(new BigDecimal("10"));
        q.setActiveFlag(true);
        return q;
    }

    private static VendorQuestionnaireResponse answer(boolean gate, EvidenceStatus status) {
        VendorQuestionnaireResponse r = new VendorQuestionnaireResponse();
        r.setQuestionCode("VS-Q-IATF");
        r.setGatePassed(gate);
        r.setEvidenceStatus(status);
        r.setScoredValue(gate ? BigDecimal.ONE : BigDecimal.ZERO);
        r.setRespondedAt(Instant.now());
        return r;
    }

    @Test
    void mandatoryGateWithPendingEvidence_fails() {
        var out = engine.evaluate(List.of(gateQuestion()), List.of(answer(true, EvidenceStatus.PENDING)));
        assertThat(out.overallEligible()).isFalse();
        assertThat(out.failedGateCodes()).contains("VS-Q-IATF");
        assertThat(out.evidencePendingCount()).isEqualTo(1);
    }

    @Test
    void mandatoryGateWithVerifiedEvidence_passes() {
        var out = engine.evaluate(List.of(gateQuestion()), List.of(answer(true, EvidenceStatus.VERIFIED)));
        assertThat(out.overallEligible()).isTrue();
        assertThat(out.failedGateCodes()).isEmpty();
    }

    @Test
    void mandatoryGateWithWaivedEvidence_passes() {
        var out = engine.evaluate(List.of(gateQuestion()), List.of(answer(true, EvidenceStatus.WAIVED)));
        assertThat(out.overallEligible()).isTrue();
    }

    @Test
    void verifiedEvidenceButAnswerGateFailed_fails() {
        var out = engine.evaluate(List.of(gateQuestion()), List.of(answer(false, EvidenceStatus.VERIFIED)));
        assertThat(out.overallEligible()).isFalse();
    }
}
