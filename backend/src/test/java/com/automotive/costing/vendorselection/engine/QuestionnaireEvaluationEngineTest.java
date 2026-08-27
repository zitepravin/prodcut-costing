package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.QuestionAnswerType;
import com.automotive.costing.vendorselection.entity.QuestionCategory;
import com.automotive.costing.vendorselection.entity.VendorQuestion;
import com.automotive.costing.vendorselection.entity.VendorQuestionnaireResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionnaireEvaluationEngineTest {

    private final QuestionnaireEvaluationEngine engine = new QuestionnaireEvaluationEngine();

    @Test
    void allRequiredAndGatesPassed_eligible() {
        VendorQuestion req = question("Q1", true, true, new BigDecimal("10"));
        VendorQuestionnaireResponse ans = answer("Q1", true, new BigDecimal("1.0"));

        var out = engine.evaluate(List.of(req), List.of(ans));
        assertThat(out.overallEligible()).isTrue();
        assertThat(out.mandatoryGatesPassed()).isTrue();
        assertThat(out.requiredAnswered()).isTrue();
        assertThat(out.mandatoryGateFailed()).isEqualTo(0);
    }

    @Test
    void missingRequired_notEligible() {
        VendorQuestion req = question("Q1", true, false, null);
        var out = engine.evaluate(List.of(req), List.of());
        assertThat(out.overallEligible()).isFalse();
        assertThat(out.requiredAnswered()).isFalse();
        assertThat(out.missingQuestionCodes()).contains("Q1");
    }

    @Test
    void failedMandatoryGate_notEligible() {
        VendorQuestion gate = question("G1", false, true, new BigDecimal("5"));
        VendorQuestionnaireResponse ans = answer("G1", false, BigDecimal.ZERO);

        var out = engine.evaluate(List.of(gate), List.of(ans));
        assertThat(out.overallEligible()).isFalse();
        assertThat(out.mandatoryGatesPassed()).isFalse();
        assertThat(out.failedGateCodes()).contains("G1");
    }

    @Test
    void unansweredMandatoryGate_fails() {
        VendorQuestion gate = question("G2", false, true, null);
        var out = engine.evaluate(List.of(gate), List.of());
        assertThat(out.failedGateCodes()).contains("G2");
        assertThat(out.overallEligible()).isFalse();
    }

    @Test
    void weightedScore_usesAnswerScoreTimesWeight() {
        VendorQuestion q = question("S1", false, false, new BigDecimal("20"));
        VendorQuestionnaireResponse ans = answer("S1", true, new BigDecimal("0.5"));

        var out = engine.evaluate(List.of(q), List.of(ans));
        assertThat(out.weightedScore()).isEqualByComparingTo("10.0000");
        assertThat(out.maxPossibleScore()).isEqualByComparingTo("20.0000");
    }

    @Test
    void missingAnswer_doesNotInventScore() {
        VendorQuestion q = question("S2", false, false, new BigDecimal("15"));
        var out = engine.evaluate(List.of(q), List.of());
        assertThat(out.weightedScore()).isEqualByComparingTo("0.0000");
        assertThat(out.maxPossibleScore()).isEqualByComparingTo("15.0000");
        assertThat(out.answeredCount()).isEqualTo(0);
    }

    private static VendorQuestion question(String code, boolean required, boolean mandatory, BigDecimal weight) {
        VendorQuestion q = new VendorQuestion();
        q.setQuestionId(code + "-id");
        q.setQuestionCode(code);
        q.setVersionNo(1);
        q.setCategory(QuestionCategory.QUALITY);
        q.setAnswerType(QuestionAnswerType.YES_NO);
        q.setRequiredFlag(required);
        q.setMandatoryGate(mandatory);
        q.setWeight(weight);
        q.setActiveFlag(true);
        return q;
    }

    private static VendorQuestionnaireResponse answer(String code, boolean gatePassed, BigDecimal score) {
        VendorQuestionnaireResponse r = new VendorQuestionnaireResponse();
        r.setResponseId(code + "-r");
        r.setQuestionCode(code);
        r.setQuestionVersion(1);
        r.setGatePassed(gatePassed);
        r.setScoredValue(score);
        r.setEvidenceStatus(EvidenceStatus.PENDING);
        return r;
    }
}
