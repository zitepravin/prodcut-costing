package com.automotive.costing.fta.engine;
import com.automotive.costing.fta.entity.TradeRemedyRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
/** Independent of FTA preference. */
public final class TradeRemedyEngine {
  private static final int SCALE = 6;
  private TradeRemedyEngine() {}
  public record RemedyLine(String measureType, BigDecimal amount, String formula) {}
  public static List<RemedyLine> apply(List<TradeRemedyRule> rules, BigDecimal customsValue, BigDecimal quantity) {
    List<RemedyLine> out = new ArrayList<>();
    if (rules == null) return out;
    for (TradeRemedyRule r : rules) {
      BigDecimal amt;
      if (r.getSpecificAmount() != null) {
        amt = r.getSpecificAmount().multiply(quantity == null ? BigDecimal.ONE : quantity).setScale(SCALE, RoundingMode.HALF_UP);
      } else if (r.getRate() != null) {
        amt = (customsValue == null ? BigDecimal.ZERO : customsValue).multiply(r.getRate()).setScale(SCALE, RoundingMode.HALF_UP);
      } else continue;
      out.add(new RemedyLine(r.getMeasureType(), amt, r.getMeasureType() + " rate/amount"));
    }
    return out;
  }
}
