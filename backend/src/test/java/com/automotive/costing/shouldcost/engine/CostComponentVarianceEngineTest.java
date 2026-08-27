package com.automotive.costing.shouldcost.engine;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal; import java.util.List; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CostComponentVarianceEngineTest {
  private static final List<CostComponentVarianceEngine.ComponentDefinitionView> DEFS = List.of(
      new CostComponentVarianceEngine.ComponentDefinitionView("MATERIAL", "Material", 10, "material_cost"),
      new CostComponentVarianceEngine.ComponentDefinitionView("PROCESS", "Process", 20, "process_cost"),
      new CostComponentVarianceEngine.ComponentDefinitionView("TOTAL", "Total", 999, "total_cost"));
  @Test void totalOnly_dataNotAvailable() {
    var r = CostComponentVarianceEngine.calculate(DEFS,
        Map.of("material_cost", new BigDecimal("40"), "process_cost", new BigDecimal("40"), "total_cost", new BigDecimal("80")),
        Map.of(), new BigDecimal("100"));
    assertTrue(r.explanation().contains("Component-level quote splits are not available"));
    assertEquals(0, new BigDecimal("20.000000").compareTo(r.absoluteVarianceTotal()));
  }
  @Test void ranksTopContributor() {
    var r = CostComponentVarianceEngine.calculate(DEFS,
        Map.of("material_cost", new BigDecimal("40"), "process_cost", new BigDecimal("40"), "total_cost", new BigDecimal("80")),
        Map.of("MATERIAL", new BigDecimal("60"), "PROCESS", new BigDecimal("41")), new BigDecimal("101"));
    assertEquals("MATERIAL", r.topContributors().get(0));
  }
  @Test void rejectsNegativeQuote() {
    assertThrows(BusinessRuleViolationException.class, () ->
        CostComponentVarianceEngine.calculate(DEFS, Map.of("total_cost", BigDecimal.TEN), Map.of(), new BigDecimal("-1")));
  }
}
