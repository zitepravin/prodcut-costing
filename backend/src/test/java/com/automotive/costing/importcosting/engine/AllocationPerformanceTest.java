package com.automotive.costing.importcosting.engine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Measured allocation scale tests — pure in-memory, no DB. */
class AllocationPerformanceTest {

  @Test void reconcile_1000_lines_value() {
    int n = 1000;
    List<AllocationEngine.LineDriver> lines = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      lines.add(new AllocationEngine.LineDriver("i" + i, BigDecimal.valueOf(10 + (i % 7)),
          BigDecimal.ONE, null, null, null, null));
    }
    BigDecimal charge = new BigDecimal("9999.99");
    long t0 = System.nanoTime();
    var out = AllocationEngine.allocate(charge, "VALUE", lines);
    long ms = (System.nanoTime() - t0) / 1_000_000L;
    assertEquals(n, out.size());
    BigDecimal sum = out.stream().map(AllocationEngine.AllocationLine::allocatedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertTrue(sum.subtract(charge).abs().compareTo(new BigDecimal("0.02")) <= 0,
        "sum allocated=" + sum + " charge=" + charge);
    System.out.println("ALLOC_PERF n=1000 ms=" + ms);
    assertTrue(ms < 5000, "1000-line allocation should be < 5s, was " + ms);
  }

  @Test void reconcile_10000_lines_quantity() {
    int n = 10_000;
    List<AllocationEngine.LineDriver> lines = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      lines.add(new AllocationEngine.LineDriver("i" + i, null, BigDecimal.ONE, null, null, null, null));
    }
    BigDecimal charge = new BigDecimal("100000");
    long t0 = System.nanoTime();
    var out = AllocationEngine.allocate(charge, "QUANTITY", lines);
    long ms = (System.nanoTime() - t0) / 1_000_000L;
    assertEquals(n, out.size());
    BigDecimal sum = out.stream().map(AllocationEngine.AllocationLine::allocatedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    // equal split: each 10.000000, sum should be ~100000
    assertTrue(sum.subtract(charge).abs().compareTo(new BigDecimal("0.10")) <= 0,
        "sum=" + sum);
    System.out.println("ALLOC_PERF n=10000 ms=" + ms);
    assertTrue(ms < 15000, "10k-line allocation should be < 15s, was " + ms);
  }
}
