package com.automotive.costing.importcosting.engine;
import com.automotive.costing.importcosting.entity.ImportTaxRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
/** Pure import tax calculation — recoverable vs non-recoverable kept separate. */
public final class TaxEngine {
  private static final int SCALE = 6;
  private TaxEngine() {}

  public record TaxLine(String taxCode, String taxType, BigDecimal amount, boolean recoverable,
                        boolean includedInLanded, String formulaNote) {}

  public static List<TaxLine> calculate(List<ImportTaxRule> rules, BigDecimal customsValue, BigDecimal dutyAmount) {
    List<TaxLine> out = new ArrayList<>();
    if (rules == null) return out;
    BigDecimal basePlusDuty = nz(customsValue).add(nz(dutyAmount));
    for (ImportTaxRule r : rules) {
      BigDecimal base = switch (r.getTaxBase() == null ? "" : r.getTaxBase().toUpperCase()) {
        case "CUSTOMS_VALUE" -> nz(customsValue);
        case "DUTY" -> nz(dutyAmount);
        case "CUSTOMS_VALUE_PLUS_DUTY" -> basePlusDuty;
        default -> basePlusDuty;
      };
      BigDecimal amt = base.multiply(nz(r.getRate())).setScale(SCALE, RoundingMode.HALF_UP);
      out.add(new TaxLine(r.getTaxCode(), r.getTaxType(), amt, r.isRecoverable(), r.isIncludedInLanded(),
          r.getTaxBase() + " * " + r.getRate()));
    }
    return out;
  }

  private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
