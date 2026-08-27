package com.automotive.costing.fta.engine;
import com.automotive.costing.fta.entity.TradeAgreement;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class AgreementEffectivenessEngineTest {
  @Test void g3_signed_not_in_force() {
    TradeAgreement a = new TradeAgreement();
    a.setStatus("SIGNED"); a.setEnabled(true);
    a.setEffectiveDate(LocalDate.of(2030,1,1));
    assertFalse(AgreementEffectivenessEngine.isPreferentialEligible(a, LocalDate.of(2026,1,1)));
  }
  @Test void g2_in_force_in_window() {
    TradeAgreement a = new TradeAgreement();
    a.setStatus("IN_FORCE"); a.setEnabled(true);
    a.setEffectiveDate(LocalDate.of(2020,1,1));
    a.setExpiryDate(LocalDate.of(2030,1,1));
    assertTrue(AgreementEffectivenessEngine.isPreferentialEligible(a, LocalDate.of(2026,6,1)));
  }
  @Test void expired() {
    TradeAgreement a = new TradeAgreement();
    a.setStatus("IN_FORCE"); a.setEnabled(true);
    a.setEffectiveDate(LocalDate.of(2010,1,1));
    a.setExpiryDate(LocalDate.of(2015,1,1));
    assertFalse(AgreementEffectivenessEngine.isPreferentialEligible(a, LocalDate.of(2026,1,1)));
  }
}
