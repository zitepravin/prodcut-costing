package com.automotive.costing.makevsbuy.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.automotive.costing.makevsbuy.engine.MakeVsBuyComparisonEngine.*;
import static org.junit.jupiter.api.Assertions.*;

class MakeVsBuyComparisonEngineTest {

    @Test
    void not_eligible_buy_cannot_win_even_if_cheaper() {
        var legs = List.of(
                leg(1, MAKE, "100", ELIGIBLE, AVAILABLE, null),
                leg(2, BUY, "50", NOT_ELIGIBLE, AVAILABLE, null)
        );
        Recommendation r = recommend(legs);
        assertEquals(MAKE, r.recommendationCode());
        assertTrue(r.notes().stream().anyMatch(n -> n.contains("NOT_ELIGIBLE")));
    }

    @Test
    void eligible_cheaper_buy_wins_on_unit_cost() {
        var legs = List.of(
                leg(1, MAKE, "100", ELIGIBLE, AVAILABLE, null),
                leg(2, BUY, "80", ELIGIBLE, AVAILABLE, null)
        );
        Recommendation r = recommend(legs);
        assertEquals(BUY, r.recommendationCode());
        assertEquals("UNIT_COST", r.preferredMetric());
    }

    @Test
    void tco_used_when_all_preferred_have_tco() {
        var legs = List.of(
                leg(1, MAKE, "100", ELIGIBLE, AVAILABLE, null),
                new LegView(2, BUY, "q1", "s1", new BigDecimal("90"), "INR",
                        new BigDecimal("110"), AVAILABLE, ELIGIBLE, null, AVAILABLE, null),
                new LegView(3, BUY, "q2", "s2", new BigDecimal("95"), "INR",
                        new BigDecimal("105"), AVAILABLE, ELIGIBLE, null, AVAILABLE, null)
        );
        Recommendation r = recommend(legs);
        assertEquals("TCO", r.preferredMetric());
        // best TCO is 105 (leg 3) vs MAKE 100 → MAKE wins
        assertEquals(MAKE, r.recommendationCode());
    }

    @Test
    void missing_all_data_inconclusive() {
        var legs = List.of(
                leg(1, MAKE, null, ELIGIBLE, NOT_AVAILABLE, null)
        );
        Recommendation r = recommend(legs);
        assertEquals(INCONCLUSIVE, r.recommendationCode());
    }

    private static LegView leg(int seq, String type, String unit, String elig, String data, String tco) {
        return new LegView(seq, type, "ref-" + seq, type.equals(BUY) ? "sup" : null,
                unit == null ? null : new BigDecimal(unit), "INR",
                tco == null ? null : new BigDecimal(tco),
                tco == null ? NOT_AVAILABLE : AVAILABLE,
                elig, null, data, null);
    }
}
