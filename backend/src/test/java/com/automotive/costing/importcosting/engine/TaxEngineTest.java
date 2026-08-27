package com.automotive.costing.importcosting.engine;
import com.automotive.costing.importcosting.entity.ImportTaxRule;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class TaxEngineTest {
  @Test void recoverableAndNonRecoverable() {
    ImportTaxRule gst = tax("GST", "0.18", true, true);
    ImportTaxRule cess = tax("CESS", "0.01", false, true);
    var lines = TaxEngine.calculate(List.of(gst, cess), new BigDecimal("1000"), new BigDecimal("100"));
    assertEquals(2, lines.size());
    assertEquals(0, new BigDecimal("198.000000").compareTo(lines.get(0).amount()));
    assertTrue(lines.get(0).recoverable());
    assertEquals(0, new BigDecimal("11.000000").compareTo(lines.get(1).amount()));
    assertFalse(lines.get(1).recoverable());
  }
  private static ImportTaxRule tax(String code, String rate, boolean recoverable, boolean included) {
    ImportTaxRule r = new ImportTaxRule();
    r.setTaxCode(code);
    r.setTaxType(code);
    r.setRate(new BigDecimal(rate));
    r.setTaxBase("CUSTOMS_VALUE_PLUS_DUTY");
    r.setRecoverable(recoverable);
    r.setIncludedInLanded(included);
    return r;
  }
}
