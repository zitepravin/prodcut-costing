package com.automotive.costing.importcosting.engine;
import com.automotive.costing.importcosting.domain.DutyType;
import com.automotive.costing.importcosting.entity.ImportDutyRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
/** Pure duty calculation — no FTA / preferential logic. */
public final class DutyEngine {
  private static final int SCALE = 6;
  private DutyEngine() {}

  public record DutyResult(BigDecimal dutyAmount, String dutyType, String formulaNote, boolean missingRule) {}

  public static DutyResult calculate(Optional<ImportDutyRule> ruleOpt, BigDecimal customsValue, BigDecimal quantity) {
    if (ruleOpt.isEmpty()) {
      return new DutyResult(null, null, "No approved effective duty rule", true);
    }
    ImportDutyRule r = ruleOpt.get();
    String type = r.getDutyType() == null ? "" : r.getDutyType().trim().toUpperCase();
    BigDecimal cv = nz(customsValue);
    BigDecimal qty = nz(quantity);
    return switch (type) {
      case "EXEMPT" -> new DutyResult(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP), type, "EXEMPT", false);
      case "AD_VALOREM" -> {
        BigDecimal rate = nz(r.getAdValoremRate());
        BigDecimal amt = cv.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
        yield new DutyResult(amt, type, "customsValue * " + rate, false);
      }
      case "SPECIFIC" -> {
        BigDecimal unit = nz(r.getSpecificAmount());
        BigDecimal amt = unit.multiply(qty).setScale(SCALE, RoundingMode.HALF_UP);
        yield new DutyResult(amt, type, "specificAmount * qty", false);
      }
      case "MIXED" -> {
        BigDecimal rate = nz(r.getAdValoremRate());
        BigDecimal unit = nz(r.getSpecificAmount());
        BigDecimal amt = cv.multiply(rate).add(unit.multiply(qty)).setScale(SCALE, RoundingMode.HALF_UP);
        yield new DutyResult(amt, type, "adValorem + specific*qty", false);
      }
      case "TIERED" -> {
        // Tiered: treat as ad valorem when only rate provided (full tier table is config extension)
        BigDecimal rate = nz(r.getAdValoremRate());
        BigDecimal amt = cv.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
        yield new DutyResult(amt, type, "tiered(simplified ad_valorem) * customsValue", false);
      }
      default -> new DutyResult(null, type, "Unsupported duty type: " + type, true);
    };
  }

  private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
