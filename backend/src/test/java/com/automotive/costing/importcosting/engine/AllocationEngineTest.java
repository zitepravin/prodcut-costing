package com.automotive.costing.importcosting.engine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class AllocationEngineTest {
  @Test void allocateByValue() {
    var lines = List.of(
        new AllocationEngine.LineDriver("a", new BigDecimal("70"), BigDecimal.ONE, null, null, null, null),
        new AllocationEngine.LineDriver("b", new BigDecimal("30"), BigDecimal.ONE, null, null, null, null)
    );
    var out = AllocationEngine.allocate(new BigDecimal("100"), "VALUE", lines);
    assertEquals(2, out.size());
    assertEquals(0, new BigDecimal("70.000000").compareTo(out.get(0).allocatedAmount()));
    assertEquals(0, new BigDecimal("30.000000").compareTo(out.get(1).allocatedAmount()));
  }
  @Test void allocateByQuantity() {
    var lines = List.of(
        new AllocationEngine.LineDriver("a", null, new BigDecimal("2"), null, null, null, null),
        new AllocationEngine.LineDriver("b", null, new BigDecimal("2"), null, null, null, null)
    );
    var out = AllocationEngine.allocate(new BigDecimal("40"), "QUANTITY", lines);
    assertEquals(0, new BigDecimal("20.000000").compareTo(out.get(0).allocatedAmount()));
  }
}
