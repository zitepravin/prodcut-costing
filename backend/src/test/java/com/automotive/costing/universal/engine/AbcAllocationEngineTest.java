package com.automotive.costing.universal.engine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal; import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class AbcAllocationEngineTest {
  private final AbcAllocationEngine engine = new AbcAllocationEngine();
  @Test void abc_golden() {
    var r = engine.allocate(
      List.of(new AbcAllocationEngine.PoolInput("SETUP", new BigDecimal("10000"), new BigDecimal("1000"))),
      List.of(new AbcAllocationEngine.UsageInput("SETUP", new BigDecimal("50"))));
    assertEquals(0, new BigDecimal("500.000000").compareTo(r.totalAllocated()));
  }
}
