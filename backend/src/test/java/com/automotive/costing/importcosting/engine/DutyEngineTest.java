package com.automotive.costing.importcosting.engine;
import com.automotive.costing.importcosting.entity.ImportDutyRule;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
class DutyEngineTest {
  @Test void adValorem() {
    ImportDutyRule r = rule("AD_VALOREM", "0.10", null);
    var res = DutyEngine.calculate(Optional.of(r), new BigDecimal("1000"), new BigDecimal("10"));
    assertFalse(res.missingRule());
    assertEquals(0, new BigDecimal("100.000000").compareTo(res.dutyAmount()));
  }
  @Test void specific() {
    ImportDutyRule r = rule("SPECIFIC", null, "5");
    var res = DutyEngine.calculate(Optional.of(r), new BigDecimal("1000"), new BigDecimal("10"));
    assertEquals(0, new BigDecimal("50.000000").compareTo(res.dutyAmount()));
  }
  @Test void mixed() {
    ImportDutyRule r = rule("MIXED", "0.05", "2");
    var res = DutyEngine.calculate(Optional.of(r), new BigDecimal("1000"), new BigDecimal("10"));
    // 50 + 20 = 70
    assertEquals(0, new BigDecimal("70.000000").compareTo(res.dutyAmount()));
  }
  @Test void exempt() {
    ImportDutyRule r = rule("EXEMPT", null, null);
    var res = DutyEngine.calculate(Optional.of(r), new BigDecimal("1000"), BigDecimal.ONE);
    assertEquals(0, BigDecimal.ZERO.compareTo(res.dutyAmount()));
  }
  @Test void missingRule() {
    var res = DutyEngine.calculate(Optional.empty(), BigDecimal.TEN, BigDecimal.ONE);
    assertTrue(res.missingRule());
    assertNull(res.dutyAmount());
  }
  private static ImportDutyRule rule(String type, String rate, String specific) {
    ImportDutyRule r = new ImportDutyRule();
    r.setDutyType(type);
    if (rate != null) r.setAdValoremRate(new BigDecimal(rate));
    if (specific != null) r.setSpecificAmount(new BigDecimal(specific));
    return r;
  }
}
