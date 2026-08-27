package com.automotive.costing.fta.engine;
import com.automotive.costing.fta.entity.TariffConcession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
/** Preferential vs normal duty — does not replace V279 DutyEngine. */
public final class TariffConcessionEngine {
  private static final int SCALE = 6;
  private TariffConcessionEngine() {}

  public record DutyCompare(BigDecimal normalDuty, BigDecimal preferentialDuty,
                            BigDecimal benefit, BigDecimal benefitPercent,
                            String rateType, boolean applied) {}

  public static DutyCompare compare(Optional<TariffConcession> concession,
                                    BigDecimal customsValue, BigDecimal quantity) {
    if (concession.isEmpty()) {
      return new DutyCompare(null, null, null, null, null, false);
    }
    TariffConcession c = concession.get();
    BigDecimal cv = nz(customsValue);
    BigDecimal qty = nz(quantity);
    BigDecimal normal = rateAmount(c.getNormalRate(), c.getRateType(), c.getSpecificAmount(), cv, qty);
    BigDecimal pref = rateAmount(c.getPreferentialRate(), c.getRateType(), c.getSpecificAmount(), cv, qty);
    if (pref == null && c.getPreferentialRate() == null && c.getSpecificAmount() == null) {
      return new DutyCompare(normal, null, null, null, c.getRateType(), false);
    }
    BigDecimal benefit = (normal != null && pref != null) ? normal.subtract(pref).setScale(SCALE, RoundingMode.HALF_UP) : null;
    BigDecimal pct = (benefit != null && normal != null && normal.compareTo(BigDecimal.ZERO) > 0)
        ? benefit.divide(normal, 8, RoundingMode.HALF_UP) : null;
    return new DutyCompare(normal, pref, benefit, pct, c.getRateType(), true);
  }

  private static BigDecimal rateAmount(BigDecimal rate, String type, BigDecimal specific,
                                       BigDecimal cv, BigDecimal qty) {
    String t = type == null ? "PERCENTAGE" : type.toUpperCase();
    return switch (t) {
      case "ZERO" -> BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
      case "SPECIFIC" -> nz(specific).multiply(qty).setScale(SCALE, RoundingMode.HALF_UP);
      case "MIXED" -> nz(cv).multiply(nz(rate)).add(nz(specific).multiply(qty)).setScale(SCALE, RoundingMode.HALF_UP);
      default -> rate == null ? null : nz(cv).multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
    };
  }
  private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
