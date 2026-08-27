package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.entity.ConfidenceLevel;
import com.automotive.costing.vendorselection.engine.RecommendationExplainabilityEngine.ExplanationInput;
import com.automotive.costing.vendorselection.engine.RecommendationRankingEngine.RankedCandidate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationExplainabilityEngineTest {

    private final RecommendationExplainabilityEngine engine = new RecommendationExplainabilityEngine();

    @Test
    void buildsDecisionBoundaryAndClassifications() {
        RankedCandidate a = new RankedCandidate("S1", 1, true, new BigDecimal("90"),
                new BigDecimal("100"), true, List.of(), "Eligible; Score 90", null, "QUALIFIED", false);
        RankedCandidate b = new RankedCandidate("S2", 2, false, null,
                null, false, List.of("OPEN_RISK"), null, "Ineligible; Failed gates: OPEN_RISK", "BLOCKED", true);

        var out = engine.build(new ExplanationInput(
                "S1", ConfidenceLevel.MEDIUM, "Draft recommendation: supplier S1",
                "Score missing for S2", List.of(a, b)));

        assertThat(out.decisionBoundaryNote()).contains("Human approve");
        assertThat(out.facts().get("classification")).isEqualTo("FACT");
        assertThat(out.calculated().get("classification")).isEqualTo("CALCULATED RESULT");
        assertThat(out.inferences().get("classification")).isEqualTo("AI INFERENCE");
        assertThat(out.missing().get("classification")).isEqualTo("MISSING DATA");
        assertThat(out.eligibilitySummary()).contains("S1=ELIGIBLE");
        assertThat(out.eligibilitySummary()).contains("S2=NOT_ELIGIBLE");
        assertThat(out.missingDataSummary()).contains("Score missing");
        assertThat(out.riskSummary()).contains("OPEN_RISK");
    }

    @Test
    void doesNotInventRecommendedSupplierWhenNone() {
        var out = engine.build(new ExplanationInput(
                null, ConfidenceLevel.LOW, "No eligible supplier",
                "No supplier candidates", List.of()));
        assertThat(out.facts().get("recommendedSupplierId")).isNull();
        assertThat(out.dataUsedSummary()).contains("Candidates evaluated: 0");
    }
}
