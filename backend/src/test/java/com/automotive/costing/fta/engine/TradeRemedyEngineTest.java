package com.automotive.costing.fta.engine;
import com.automotive.costing.fta.entity.TradeRemedyRule;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class TradeRemedyEngineTest {
  @Test void g13_independent_of_fta() {
    TradeRemedyRule r = new TradeRemedyRule();
    r.setMeasureType("ANTI_DUMPING");
    r.setRate(new BigDecimal("0.15"));
    var lines = TradeRemedyEngine.apply(List.of(r), new BigDecimal("1000"), BigDecimal.ONE);
    assertEquals(1, lines.size());
    assertEquals(0, new BigDecimal("150.000000").compareTo(lines.get(0).amount()));
  }
}
