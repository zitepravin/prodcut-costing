package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.vendorselection.engine.SupplierComparisonEngine.SupplierFacts;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierComparisonEngineTest {

    private final SupplierComparisonEngine engine = new SupplierComparisonEngine();

    @Test
    void marksMissingAsDataNotAvailable_andSurfacesTradeOff() {
        var out = engine.compare(List.of(
                new SupplierFacts("A", "Alpha", true, List.of(), new BigDecimal("90"),
                        new BigDecimal("200"), true, List.of(), new BigDecimal("10"),
                        new BigDecimal("95"), 24, 3, List.of(), "QUALIFIED"),
                new SupplierFacts("B", "Beta", true, List.of(), new BigDecimal("80"),
                        new BigDecimal("150"), true, List.of(), null,
                        null, null, 1, List.of(), "QUALIFIED")
        ));
        assertThat(out.supplierIds()).containsExactly("A", "B");
        assertThat(out.rows()).isNotEmpty();
        var tcoRow = out.rows().stream().filter(r -> "TCO_TOTAL".equals(r.dimension())).findFirst().orElseThrow();
        assertThat(tcoRow.values().get(0)).isEqualTo("200");
        assertThat(tcoRow.values().get(1)).isEqualTo("150");
        var ppmRow = out.rows().stream().filter(r -> "LATEST_PPM".equals(r.dimension())).findFirst().orElseThrow();
        assertThat(ppmRow.values().get(1)).isEqualTo("DATA_NOT_AVAILABLE");
        assertThat(out.tradeOffSummary()).contains("does not approve");
        assertThat(out.tradeOffSummary()).contains("Score leader and TCO leader differ");
        assertThat(out.lifecycleBlockedSupplierIds()).isEmpty();
    }

    @Test
    void noEligible_message() {
        var out = engine.compare(List.of(
                new SupplierFacts("X", "X", false, List.of("CERT"), null, null, false,
                        List.of(), null, null, null, 0, List.of("No capabilities"), null)
        ));
        assertThat(out.tradeOffSummary()).contains("No eligible");
    }

    @Test
    void blockedLifecycle_forcedIneligible_andListed() {
        var out = engine.compare(List.of(
                new SupplierFacts("B1", "Blocked Co", true, List.of(), new BigDecimal("99"),
                        new BigDecimal("10"), true, List.of(), null, null, null, 5, List.of(), "BLOCKED"),
                new SupplierFacts("G1", "Good Co", true, List.of(), new BigDecimal("70"),
                        new BigDecimal("20"), true, List.of(), null, null, null, 2, List.of(), "APPROVED")
        ));
        assertThat(out.lifecycleBlockedSupplierIds()).containsExactly("B1");
        var eligibleRow = out.rows().stream().filter(r -> "ELIGIBLE".equals(r.dimension())).findFirst().orElseThrow();
        assertThat(eligibleRow.values().get(0)).isEqualTo("false");
        assertThat(eligibleRow.values().get(1)).isEqualTo("true");
        assertThat(out.tradeOffSummary()).contains("BLOCKED/SUSPENDED");
        // Preference among eligible only - G1
        assertThat(out.tradeOffSummary()).contains("G1");
    }

    @Test
    void suspendedLifecycle_forcedIneligible() {
        var out = engine.compare(List.of(
                new SupplierFacts("S1", "Suspended", true, List.of(), new BigDecimal("95"),
                        new BigDecimal("5"), true, List.of(), null, null, null, 1, List.of(), "SUSPENDED")
        ));
        assertThat(out.lifecycleBlockedSupplierIds()).contains("S1");
        assertThat(out.tradeOffSummary()).contains("No eligible");
    }
}
