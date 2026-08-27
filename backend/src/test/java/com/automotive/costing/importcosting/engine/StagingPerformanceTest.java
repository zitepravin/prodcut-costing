package com.automotive.costing.importcosting.engine;
import com.automotive.costing.fta.engine.OriginRuleEngine;
import com.automotive.costing.fta.engine.TariffConcessionEngine;
import com.automotive.costing.fta.entity.OriginRule;
import com.automotive.costing.fta.entity.TariffConcession;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** V283 staging-scale pure-engine performance (no DB). */
class StagingPerformanceTest {

  @Test void allocate_10k_lines() {
    int n = 10_000;
    List<AllocationEngine.LineDriver> lines = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      lines.add(new AllocationEngine.LineDriver("L"+i, BigDecimal.valueOf(1 + (i % 50)),
          BigDecimal.ONE, null, null, null, null));
    }
    BigDecimal charge = new BigDecimal("250000.00");
    long t0 = System.nanoTime();
    var out = AllocationEngine.allocate(charge, "VALUE", lines);
    long ms = (System.nanoTime() - t0) / 1_000_000L;
    assertEquals(n, out.size());
    BigDecimal sum = out.stream().map(AllocationEngine.AllocationLine::allocatedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertTrue(sum.subtract(charge).abs().compareTo(new BigDecimal("0.50")) <= 0);
    System.out.println("PERF_ALLOC_10k_ms=" + ms);
    assertTrue(ms < 30_000, "10k allocation < 30s was " + ms);
  }

  @Test void tariff_compare_10k() {
    TariffConcession c = new TariffConcession();
    c.setRateType("PERCENTAGE");
    c.setNormalRate(new BigDecimal("0.10"));
    c.setPreferentialRate(new BigDecimal("0.00"));
    long t0 = System.nanoTime();
    for (int i = 0; i < 10_000; i++) {
      TariffConcessionEngine.compare(Optional.of(c), BigDecimal.valueOf(1000 + i), BigDecimal.TEN);
    }
    long ms = (System.nanoTime() - t0) / 1_000_000L;
    System.out.println("PERF_TARIFF_10k_ms=" + ms);
    assertTrue(ms < 15_000);
  }

  @Test void origin_assess_10k() {
    OriginRule r = new OriginRule();
    r.setRuleType("RVC");
    r.setRvcThresholdPct(new BigDecimal("35"));
    r.setCumulationAllowed(true);
    long t0 = System.nanoTime();
    for (int i = 0; i < 10_000; i++) {
      OriginRuleEngine.assess(Optional.of(r),
          new OriginRuleEngine.OriginInputs("VN","VN","7208","7208",
              new BigDecimal("100"), new BigDecimal("30"), true, false, true, false));
    }
    long ms = (System.nanoTime() - t0) / 1_000_000L;
    System.out.println("PERF_ORIGIN_10k_ms=" + ms);
    assertTrue(ms < 15_000);
  }
}
