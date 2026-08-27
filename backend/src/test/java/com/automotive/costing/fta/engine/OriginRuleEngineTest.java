package com.automotive.costing.fta.engine;
import com.automotive.costing.fta.domain.OriginResult;
import com.automotive.costing.fta.entity.OriginRule;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
class OriginRuleEngineTest {
  @Test void g5_proof_missing_review() {
    OriginRule r = rvcRule();
    var d = OriginRuleEngine.assess(Optional.of(r), inputs(false, false, false));
    assertEquals(OriginResult.REVIEW, d.result());
  }
  @Test void g7_valid_rvc() {
    OriginRule r = rvcRule();
    var d = OriginRuleEngine.assess(Optional.of(r),
        new OriginRuleEngine.OriginInputs("VN","VN","7208.51","7208.51",
            new BigDecimal("100"), new BigDecimal("40"), true, false, true, false));
    assertEquals(OriginResult.ELIGIBLE, d.result());
    assertTrue(d.rvcPct().compareTo(new BigDecimal("35")) >= 0);
  }
  @Test void g8_invalid_rvc() {
    OriginRule r = rvcRule();
    var d = OriginRuleEngine.assess(Optional.of(r),
        new OriginRuleEngine.OriginInputs("VN","VN","7208","7208",
            new BigDecimal("100"), new BigDecimal("80"), true, false, true, false));
    assertEquals(OriginResult.NOT_ELIGIBLE, d.result());
  }
  @Test void g6_china_asean_no_transformation() {
    OriginRule r = new OriginRule();
    r.setRuleType("CTH"); r.setCtcLevel("CTH");
    var d = OriginRuleEngine.assess(Optional.of(r),
        new OriginRuleEngine.OriginInputs("CN","VN","7208.51","7208.52",
            null, null, true, true, false, false));
    // same heading 7208 → CTH fail + third country
    assertEquals(OriginResult.NOT_ELIGIBLE, d.result());
  }
  @Test void g9_cth_pass() {
    OriginRule r = new OriginRule();
    r.setRuleType("CTH"); r.setCtcLevel("CTH");
    var d = OriginRuleEngine.assess(Optional.of(r),
        new OriginRuleEngine.OriginInputs("JP","JP","8708.29","7208.51",
            null, null, true, false, true, false));
    assertEquals(OriginResult.ELIGIBLE, d.result());
    assertEquals("PASS", d.classificationResult());
  }
  @Test void g12_cumulation_not_allowed() {
    OriginRule r = rvcRule();
    r.setCumulationAllowed(false);
    var d = OriginRuleEngine.assess(Optional.of(r),
        new OriginRuleEngine.OriginInputs("VN","VN","7208","7208",
            new BigDecimal("100"), new BigDecimal("20"), true, false, true, true));
    assertEquals(OriginResult.NOT_ELIGIBLE, d.result());
    assertEquals("NOT_ALLOWED", d.cumulationResult());
  }
  private static OriginRule rvcRule() {
    OriginRule r = new OriginRule();
    r.setRuleType("RVC");
    r.setRvcThresholdPct(new BigDecimal("35"));
    r.setRvcFormulaCode("FOB_VNM");
    r.setCumulationAllowed(true);
    return r;
  }
  private static OriginRuleEngine.OriginInputs inputs(boolean proof, boolean third, boolean transform) {
    return new OriginRuleEngine.OriginInputs("CN","SG","8471","8471",
        new BigDecimal("100"), new BigDecimal("50"), proof, third, transform, false);
  }
}
