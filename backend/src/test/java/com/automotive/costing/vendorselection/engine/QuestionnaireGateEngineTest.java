package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.entity.QuestionAnswerType;
import com.automotive.costing.vendorselection.entity.VendorQuestion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionnaireGateEngineTest {

    private final QuestionnaireGateEngine engine = new QuestionnaireGateEngine();

    @Test
    void mandatoryYesNo_failsOnNo() {
        VendorQuestion q = new VendorQuestion();
        q.setAnswerType(QuestionAnswerType.YES_NO);
        q.setMandatoryGate(true);
        var r = engine.evaluate(q, false, null, null);
        assertThat(r.gatePassed()).isFalse();
    }

    @Test
    void mandatoryYesNo_passesOnYes() {
        VendorQuestion q = new VendorQuestion();
        q.setAnswerType(QuestionAnswerType.YES_NO);
        q.setMandatoryGate(true);
        var r = engine.evaluate(q, true, null, null);
        assertThat(r.gatePassed()).isTrue();
        assertThat(r.scoredValue()).isEqualByComparingTo("100");
    }

    @Test
    void nonMandatory_nullGate() {
        VendorQuestion q = new VendorQuestion();
        q.setAnswerType(QuestionAnswerType.TEXT);
        q.setMandatoryGate(false);
        var r = engine.evaluate(q, null, null, "ok");
        assertThat(r.gatePassed()).isNull();
    }
}
