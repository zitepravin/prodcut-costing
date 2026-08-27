package com.automotive.costing.fta.engine;
import com.automotive.costing.fta.entity.TariffConcession;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
class TariffConcessionEngineTest {
  @Test void preferentialBenefit() {
    TariffConcession c = new TariffConcession();
    c.setRateType("PERCENTAGE");
    c.setNormalRate(new BigDecimal("0.10"));
    c.setPreferentialRate(new BigDecimal("0.00"));
    var r = TariffConcessionEngine.compare(Optional.of(c), new BigDecimal("1000"), BigDecimal.ONE);
    assertTrue(r.applied());
    assertEquals(0, new BigDecimal("100.000000").compareTo(r.normalDuty()));
    assertEquals(0, new BigDecimal("0.000000").compareTo(r.preferentialDuty()));
    assertEquals(0, new BigDecimal("100.000000").compareTo(r.benefit()));
  }
  @Test void noConcession() {
    var r = TariffConcessionEngine.compare(Optional.empty(), BigDecimal.TEN, BigDecimal.ONE);
    assertFalse(r.applied());
  }
}
