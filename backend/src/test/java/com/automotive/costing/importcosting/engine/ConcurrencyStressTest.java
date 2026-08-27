package com.automotive.costing.importcosting.engine;
import com.automotive.costing.fta.engine.OriginRuleEngine;
import com.automotive.costing.fta.entity.OriginRule;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyStressTest {

  @Test void concurrent_allocation_levels() throws Exception {
    for (int level : new int[]{10, 25, 50, 100}) {
      ExecutorService pool = Executors.newFixedThreadPool(Math.min(level, 32));
      BigDecimal charge = new BigDecimal("50000");
      List<Future<BigDecimal>> futures = new ArrayList<>();
      long t0 = System.nanoTime();
      for (int i = 0; i < level; i++) {
        futures.add(pool.submit(() -> {
          List<AllocationEngine.LineDriver> lines = new ArrayList<>();
          for (int j = 0; j < 200; j++) {
            lines.add(new AllocationEngine.LineDriver("x"+j, BigDecimal.TEN, BigDecimal.ONE, null, null, null, null));
          }
          return AllocationEngine.allocate(charge, "VALUE", lines).stream()
              .map(AllocationEngine.AllocationLine::allocatedAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
        }));
      }
      int errors = 0;
      for (Future<BigDecimal> f : futures) {
        try {
          BigDecimal sum = f.get(60, TimeUnit.SECONDS);
          if (sum.subtract(charge).abs().compareTo(new BigDecimal("0.10")) > 0) errors++;
        } catch (Exception e) { errors++; }
      }
      long ms = (System.nanoTime() - t0) / 1_000_000L;
      pool.shutdown();
      System.out.println("CONCUR_ALLOC level=" + level + " ms=" + ms + " errors=" + errors);
      assertEquals(0, errors, "errors at level " + level);
    }
  }

  @Test void concurrent_origin_100() throws Exception {
    OriginRule r = new OriginRule();
    r.setRuleType("RVC");
    r.setRvcThresholdPct(new BigDecimal("35"));
    ExecutorService pool = Executors.newFixedThreadPool(32);
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      futures.add(pool.submit(() -> OriginRuleEngine.assess(Optional.of(r),
          new OriginRuleEngine.OriginInputs("VN","VN","7208","7208",
              new BigDecimal("100"), new BigDecimal("40"), true, false, true, false))));
    }
    for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
    pool.shutdown();
  }
}
