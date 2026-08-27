package com.automotive.costing.mixedcost.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.automotive.costing.mixedcost.engine.MixedCostBlendEngine.*;
import static org.junit.jupiter.api.Assertions.*;

class MixedCostBlendEngineTest {

    @Test
    void blend_60_40_produces_92() {
        var legs = List.of(
                leg(1, TYPE_INTERNAL, "0.6000", "100.000000", ELIGIBLE, AVAILABLE),
                leg(2, TYPE_QUOTE, "0.4000", "80.000000", ELIGIBLE, AVAILABLE)
        );
        BlendResult r = calculate(legs);
        assertTrue(r.ratioValid());
        assertFalse(r.calculateBlocked());
        assertEquals(AVAILABLE, r.blendedStatus());
        assertEquals(0, new BigDecimal("92.000000").compareTo(r.blendedUnitTotal()));
    }

    @Test
    void ratio_not_100_fails() {
        var legs = List.of(
                leg(1, TYPE_INTERNAL, "0.5000", "100", ELIGIBLE, AVAILABLE),
                leg(2, TYPE_QUOTE, "0.4000", "80", ELIGIBLE, AVAILABLE)
        );
        BlendResult r = calculate(legs);
        assertFalse(r.ratioValid());
        assertEquals(NOT_AVAILABLE, r.blendedStatus());
        assertNull(r.blendedUnitTotal());
        assertTrue(r.dataQualityNotes().stream().anyMatch(n -> n.contains("100%")));
    }

    @Test
    void missing_leg_cost_not_available() {
        var legs = List.of(
                leg(1, TYPE_INTERNAL, "0.5000", "100", ELIGIBLE, AVAILABLE),
                new LegInput(2, TYPE_QUOTE, "q2", "s2", new BigDecimal("0.5000"),
                        null, "INR", ELIGIBLE, null, NOT_AVAILABLE)
        );
        BlendResult r = calculate(legs);
        assertTrue(r.ratioValid());
        assertTrue(r.calculateBlocked());
        assertEquals(NOT_AVAILABLE, r.blendedStatus());
        assertNull(r.blendedUnitTotal());
    }

    @Test
    void blocked_supplier_blocks_calculate() {
        var legs = List.of(
                leg(1, TYPE_INTERNAL, "0.5000", "100", ELIGIBLE, AVAILABLE),
                leg(2, TYPE_QUOTE, "0.5000", "90", NOT_ELIGIBLE, AVAILABLE)
        );
        BlendResult r = calculate(legs);
        assertTrue(r.ratioValid());
        assertTrue(r.calculateBlocked());
        assertEquals(NOT_AVAILABLE, r.blendedStatus());
        assertNull(r.blendedUnitTotal());
        assertTrue(r.dataQualityNotes().stream().anyMatch(n -> n.toLowerCase().contains("not eligible")));
    }

    @Test
    void fewer_than_two_legs_rejected() {
        var legs = List.of(leg(1, TYPE_INTERNAL, "1.0000", "50", ELIGIBLE, AVAILABLE));
        BlendResult r = calculate(legs);
        assertFalse(r.ratioValid());
        assertTrue(r.calculateBlocked());
    }

    private static LegInput leg(int seq, String type, String ratio, String unit,
                                String elig, String data) {
        return new LegInput(seq, type, "ref-" + seq, type.equals(TYPE_QUOTE) ? "sup-" + seq : null,
                new BigDecimal(ratio), new BigDecimal(unit), "INR", elig, null, data);
    }
}
