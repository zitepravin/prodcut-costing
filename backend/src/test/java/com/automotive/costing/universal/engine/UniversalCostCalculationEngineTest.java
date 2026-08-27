package com.automotive.costing.universal.engine;
import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.universal.domain.CostMethodType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal; import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class UniversalCostCalculationEngineTest {
  private final UniversalCostCalculationEngine engine = new UniversalCostCalculationEngine(new AbcAllocationEngine());
  @Test void standard_sum() {
    var out = engine.calculate(new UniversalCostCalculationEngine.CalculationInput(
      CostMethodType.STANDARD,
      List.of(CostComponentResult.builder("MATERIAL").amount(new BigDecimal("100")).build(),
              CostComponentResult.builder("LABOR").amount(new BigDecimal("50")).build()),
      null, new BigDecimal("10"), null));
    assertEquals(0, new BigDecimal("150.000000").compareTo(out.totalCost()));
    assertEquals(0, new BigDecimal("15.000000").compareTo(out.unitCost()));
  }
}
