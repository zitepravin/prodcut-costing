package com.automotive.costing.fta.engine;
import com.automotive.costing.fta.domain.OriginResult;
import com.automotive.costing.fta.entity.OriginRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
/**
 * Generic origin assessment — agreement + HS + inputs + date.
 * Origin country is independent of export country.
 */
public final class OriginRuleEngine {
  private OriginRuleEngine() {}

  public record OriginDecision(OriginResult result, String reason, BigDecimal rvcPct,
                               String classificationResult, String cumulationResult) {}

  public record OriginInputs(
      String originCountry, String exportCountry, String finishedHs, String inputHs,
      BigDecimal fob, BigDecimal vnm, boolean proofVerified, boolean thirdCountryMaterial,
      boolean substantialTransformation, boolean cumulationClaimed
  ) {}

  public static OriginDecision assess(Optional<OriginRule> ruleOpt, OriginInputs in) {
    if (ruleOpt.isEmpty()) {
      return new OriginDecision(OriginResult.REVIEW, "No approved effective origin rule", null, null, null);
    }
    OriginRule rule = ruleOpt.get();
    // Origin ≠ export: never rewrite origin based on export country alone
    if (in.originCountry() == null || in.originCountry().isBlank()) {
      return new OriginDecision(OriginResult.REVIEW, "Origin country required", null, null, null);
    }
    if (!in.proofVerified()) {
      return new OriginDecision(OriginResult.REVIEW, "Proof of origin not verified", null, null, null);
    }
    String type = rule.getRuleType() == null ? "" : rule.getRuleType().toUpperCase();
    return switch (type) {
      case "WHOLLY_OBTAINED" -> {
        if (in.thirdCountryMaterial())
          yield new OriginDecision(OriginResult.NOT_ELIGIBLE, "Third-country material fails wholly obtained", null, null, null);
        yield new OriginDecision(OriginResult.ELIGIBLE, "Wholly obtained", null, "N/A", null);
      }
      case "RVC" -> assessRvc(rule, in);
      case "CTH", "CTSH", "CTC" -> assessCtc(rule, in, type);
      case "COMBINATION", "PRODUCT_SPECIFIC_RULE" -> {
        OriginDecision ctc = assessCtc(rule, in, rule.getCtcLevel() != null ? rule.getCtcLevel() : "CTH");
        if (ctc.result() != OriginResult.ELIGIBLE) yield ctc;
        yield assessRvc(rule, in);
      }
      default -> new OriginDecision(OriginResult.REVIEW, "Unsupported rule type: " + type, null, null, null);
    };
  }

  private static OriginDecision assessRvc(OriginRule rule, OriginInputs in) {
    if (in.fob() == null || in.fob().compareTo(BigDecimal.ZERO) <= 0) {
      return new OriginDecision(OriginResult.REVIEW, "FOB required for RVC", null, null, null);
    }
    BigDecimal vnm = in.vnm() == null ? BigDecimal.ZERO : in.vnm();
    // Configurable formula code — default FOB_VNM: (FOB - VNM) / FOB * 100
    BigDecimal rvc = in.fob().subtract(vnm).divide(in.fob(), 8, RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_UP);
    BigDecimal threshold = rule.getRvcThresholdPct() == null ? new BigDecimal("35") : rule.getRvcThresholdPct();
    if (rvc.compareTo(threshold) >= 0) {
      String cum = null;
      if (in.cumulationClaimed()) {
        cum = rule.isCumulationAllowed() ? "ALLOWED" : "NOT_ALLOWED";
        if (!rule.isCumulationAllowed()) {
          return new OriginDecision(OriginResult.NOT_ELIGIBLE, "Cumulation claimed but not allowed", rvc, null, cum);
        }
      }
      return new OriginDecision(OriginResult.ELIGIBLE, "RVC " + rvc + " >= " + threshold, rvc, null, cum);
    }
    return new OriginDecision(OriginResult.NOT_ELIGIBLE, "RVC " + rvc + " < " + threshold, rvc, null, null);
  }

  private static OriginDecision assessCtc(OriginRule rule, OriginInputs in, String level) {
    if (in.finishedHs() == null || in.inputHs() == null) {
      return new OriginDecision(OriginResult.REVIEW, "Input and finished HS required for " + level, null, null, null);
    }
    String fin = digits(in.finishedHs());
    String inp = digits(in.inputHs());
    boolean pass = switch (level.toUpperCase()) {
      case "CC" -> fin.length() >= 2 && inp.length() >= 2 && !fin.substring(0, 2).equals(inp.substring(0, 2));
      case "CTH" -> fin.length() >= 4 && inp.length() >= 4 && !fin.substring(0, 4).equals(inp.substring(0, 4));
      case "CTSH" -> fin.length() >= 6 && inp.length() >= 6 && !fin.substring(0, 6).equals(inp.substring(0, 6));
      default -> !fin.equals(inp);
    };
    // China→ASEAN without transformation: same HS family + third country → fail
    if (in.thirdCountryMaterial() && !in.substantialTransformation() && !pass) {
      return new OriginDecision(OriginResult.NOT_ELIGIBLE,
          "Third-country material without qualifying tariff shift / transformation", null, "FAIL", null);
    }
    if (pass) return new OriginDecision(OriginResult.ELIGIBLE, level + " tariff shift pass", null, "PASS", null);
    return new OriginDecision(OriginResult.NOT_ELIGIBLE, level + " tariff shift fail", null, "FAIL", null);
  }

  private static String digits(String hs) {
    if (hs == null) return "";
    return hs.replaceAll("[^0-9]", "");
  }
}
