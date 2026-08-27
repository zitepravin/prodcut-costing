package com.automotive.costing.importcosting.engine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
/**
 * Aggregates import components into landed cost.
 * Never fabricates missing amounts — callers mark MISSING_DATA.
 */
public final class LandedCostEngine {
  private static final int SCALE = 6;
  private LandedCostEngine() {}

  public record ComponentLine(String code, BigDecimal amount, boolean recoverable, boolean includedInLanded,
                              String formulaNote) {}
  public record LandedResult(BigDecimal invoiceValue, BigDecimal customsValue, BigDecimal totalLanded,
                             BigDecimal unitLanded, BigDecimal quantity, List<ComponentLine> components) {}

  public static LandedResult build(BigDecimal invoiceValue, BigDecimal customsValue, BigDecimal quantity,
                                   List<ComponentLine> components) {
    BigDecimal total = BigDecimal.ZERO;
    List<ComponentLine> lines = components == null ? List.of() : components;
    for (ComponentLine c : lines) {
      if (c.includedInLanded() && c.amount() != null) {
        total = total.add(c.amount());
      }
    }
    total = total.setScale(SCALE, RoundingMode.HALF_UP);
    BigDecimal qty = quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0 ? null : quantity;
    BigDecimal unit = qty == null ? null : total.divide(qty, SCALE, RoundingMode.HALF_UP);
    return new LandedResult(invoiceValue, customsValue, total, unit, quantity, new ArrayList<>(lines));
  }
}
