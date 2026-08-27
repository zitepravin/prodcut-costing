package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.engine.ScorecardScoringEngine.CategoryScoreInput;
import com.automotive.costing.vendorselection.engine.ScorecardScoringEngine.CategoryWeight;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScorecardScoringEngineTest {

    private final ScorecardScoringEngine engine = new ScorecardScoringEngine();

    @Test
    void weightsMustTotal100() {
        assertThatThrownBy(() -> engine.assertWeightsTotal100(List.of(
                new CategoryWeight("Q", "Quality", new BigDecimal("40")),
                new CategoryWeight("C", "Cost", new BigDecimal("40"))
        ))).isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("100%");
    }

    @Test
    void weightedScore_example() {
        var weights = List.of(
                new CategoryWeight("QUALITY", "Quality", new BigDecimal("30")),
                new CategoryWeight("COST", "Cost", new BigDecimal("20")),
                new CategoryWeight("DELIVERY", "Delivery", new BigDecimal("50"))
        );
        var scores = List.of(
                new CategoryScoreInput("QUALITY", new BigDecimal("80")),
                new CategoryScoreInput("COST", new BigDecimal("70")),
                new CategoryScoreInput("DELIVERY", new BigDecimal("90"))
        );
        var out = engine.score(weights, scores);
        assertThat(out.overallScore()).isEqualByComparingTo("83.0000");
        assertThat(out.lines()).hasSize(3);
    }

    @Test
    void missingCategoryScore_fails() {
        var weights = List.of(new CategoryWeight("Q", "Q", new BigDecimal("100")));
        assertThatThrownBy(() -> engine.score(weights, List.of()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rawScoreOutOfRange_fails() {
        var weights = List.of(new CategoryWeight("Q", "Q", new BigDecimal("100")));
        assertThatThrownBy(() -> engine.score(weights,
                List.of(new CategoryScoreInput("Q", new BigDecimal("120")))))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
