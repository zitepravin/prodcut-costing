package com.automotive.costing.importcosting.engine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class LandedCostEngineTest {
  @Test void totalAndUnit() {
    var comps = List.of(
        new LandedCostEngine.ComponentLine("PURCHASE_VALUE", new BigDecimal("1000"), false, true, null),
        new LandedCostEngine.ComponentLine("CUSTOMS_DUTY", new BigDecimal("100"), false, true, null),
        new LandedCostEngine.ComponentLine("RECOVERABLE_TAX", new BigDecimal("50"), true, false, null)
    );
    var r = LandedCostEngine.build(new BigDecimal("1000"), new BigDecimal("1000"), new BigDecimal("10"), comps);
    // recoverable not included
    assertEquals(0, new BigDecimal("1100.000000").compareTo(r.totalLanded()));
    assertEquals(0, new BigDecimal("110.000000").compareTo(r.unitLanded()));
  }
}
