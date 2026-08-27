package com.automotive.costing.shouldcost.engine;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import java.math.BigDecimal; import java.math.RoundingMode;
import java.util.*; import java.util.stream.Collectors;
public final class CostComponentVarianceEngine {
  private static final int SCALE = 6;
  public static final String STATUS_AVAILABLE = "AVAILABLE";
  public static final String STATUS_DATA_NOT_AVAILABLE = "DATA_NOT_AVAILABLE";
  private CostComponentVarianceEngine() {}
  public record ComponentDefinitionView(String componentCode, String displayName, int sequenceNo, String mapsToColumn) {}
  public record ComponentAmount(String componentCode, String displayName, int sequenceNo, BigDecimal shouldCostAmount,
      BigDecimal quotedAmount, BigDecimal absoluteVariance, BigDecimal percentageVariance, String dataStatus) {}
  public record DeepVarianceResult(BigDecimal shouldCostTotal, BigDecimal quotedTotal, BigDecimal absoluteVarianceTotal,
      BigDecimal percentageVarianceTotal, List<ComponentAmount> components, List<String> topContributors, String explanation) {}
  public static DeepVarianceResult calculate(List<ComponentDefinitionView> definitions, Map<String, BigDecimal> columnAmounts,
      Map<String, BigDecimal> quoteByCode, BigDecimal quotedTotal) {
    if (quotedTotal == null || quotedTotal.compareTo(BigDecimal.ZERO) < 0)
      throw new BusinessRuleViolationException("quotedTotal must be zero or greater.", "P2-01");
    BigDecimal shouldTotal = nz(columnAmounts.get("total_cost"));
    List<ComponentAmount> components = new ArrayList<>();
    for (ComponentDefinitionView def : definitions) {
      BigDecimal shouldAmt = def.mapsToColumn() != null ? nz(columnAmounts.get(def.mapsToColumn())) : BigDecimal.ZERO;
      BigDecimal quotedAmt = quoteByCode != null ? quoteByCode.get(def.componentCode()) : null;
      String status; BigDecimal absVar = null; BigDecimal pctVar = null;
      if (quotedAmt != null) {
        status = STATUS_AVAILABLE;
        absVar = quotedAmt.subtract(shouldAmt).setScale(SCALE, RoundingMode.HALF_UP);
        if (shouldAmt.compareTo(BigDecimal.ZERO) != 0) pctVar = absVar.divide(shouldAmt, SCALE, RoundingMode.HALF_UP);
      } else status = STATUS_DATA_NOT_AVAILABLE;
      components.add(new ComponentAmount(def.componentCode(), def.displayName(), def.sequenceNo(),
          shouldAmt.setScale(SCALE, RoundingMode.HALF_UP),
          quotedAmt != null ? quotedAmt.setScale(SCALE, RoundingMode.HALF_UP) : null, absVar, pctVar, status));
    }
    List<String> top = components.stream().filter(c -> !"TOTAL".equals(c.componentCode()))
        .filter(c -> c.absoluteVariance() != null)
        .sorted(Comparator.comparing((ComponentAmount c) -> c.absoluteVariance().abs()).reversed())
        .limit(3).map(ComponentAmount::componentCode).collect(Collectors.toList());
    BigDecimal absTotal = quotedTotal.subtract(shouldTotal).setScale(SCALE, RoundingMode.HALF_UP);
    BigDecimal pctTotal = shouldTotal.compareTo(BigDecimal.ZERO) != 0 ? absTotal.divide(shouldTotal, SCALE, RoundingMode.HALF_UP) : null;
    String expl = "Should-cost total " + shouldTotal.toPlainString() + " vs quoted total " + quotedTotal.toPlainString()
        + "; absolute variance " + absTotal.toPlainString() + ".";
    long available = components.stream().filter(c -> STATUS_AVAILABLE.equals(c.dataStatus())).count();
    if (available == 0) expl += " Component-level quote splits are not available; variance is shown at total level only.";
    else if (!top.isEmpty()) expl += " Largest contributing components by absolute variance: " + String.join(", ", top) + ".";
    return new DeepVarianceResult(shouldTotal.setScale(SCALE, RoundingMode.HALF_UP), quotedTotal.setScale(SCALE, RoundingMode.HALF_UP),
        absTotal, pctTotal, components, top, expl);
  }
  private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
