package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.engine.RecommendationRankingEngine.CandidateInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationRankingEngineTest {

    private final RecommendationRankingEngine engine = new RecommendationRankingEngine();

    @Test
    void ranksEligibleByScoreThenTco_ineligibleLast() {
        var out = engine.rank(List.of(
                new CandidateInput("S1", true, new BigDecimal("80"), new BigDecimal("120"), true, List.of(), List.of(), null),
                new CandidateInput("S2", true, new BigDecimal("90"), new BigDecimal("150"), true, List.of(), List.of(), null),
                new CandidateInput("S3", false, new BigDecimal("99"), new BigDecimal("50"), true, List.of("CERT"), List.of(), null)
        ));
        assertThat(out.recommendedSupplierId()).isEqualTo("S2");
        assertThat(out.ranking().get(0).supplierId()).isEqualTo("S2");
        assertThat(out.ranking().get(2).supplierId()).isEqualTo("S3");
        assertThat(out.ranking().get(2).eligible()).isFalse();
    }

    @Test
    void noEligible_recommendsNull() {
        var out = engine.rank(List.of(
                new CandidateInput("S1", false, new BigDecimal("90"), null, false, List.of("CAP"), List.of(), null)
        ));
        assertThat(out.recommendedSupplierId()).isNull();
        assertThat(out.confidenceLevel().name()).isEqualTo("LOW");
    }

    @Test
    void blockedLifecycle_forcedIneligibleEvenIfMarkedEligible() {
        var out = engine.rank(List.of(
                new CandidateInput("S1", true, new BigDecimal("95"), new BigDecimal("100"), true, List.of(), List.of(), "BLOCKED"),
                new CandidateInput("S2", true, new BigDecimal("80"), new BigDecimal("110"), true, List.of(), List.of(), "QUALIFIED")
        ));
        assertThat(out.recommendedSupplierId()).isEqualTo("S2");
        assertThat(out.ranking().stream().filter(r -> r.supplierId().equals("S1")).findFirst().orElseThrow().eligible()).isFalse();
        assertThat(out.ranking().stream().filter(r -> r.supplierId().equals("S1")).findFirst().orElseThrow().lifecycleBlocked()).isTrue();
        assertThat(out.ranking().stream().filter(r -> r.supplierId().equals("S1")).findFirst().orElseThrow().failedGates()).contains("LIFECYCLE_STATUS");
    }

    @Test
    void suspendedLifecycle_forcedIneligible() {
        var out = engine.rank(List.of(
                new CandidateInput("S1", true, new BigDecimal("99"), new BigDecimal("10"), true, List.of(), List.of(), "SUSPENDED")
        ));
        assertThat(out.recommendedSupplierId()).isNull();
        assertThat(out.ranking().get(0).eligible()).isFalse();
    }
}
