package com.automotive.costing.fta.engine;
import com.automotive.costing.fta.domain.AgreementStatus;
import com.automotive.costing.fta.entity.TradeAgreement;
import java.time.LocalDate;
/** Date/status gate — never use hard-coded "today is active" in business logic. */
public final class AgreementEffectivenessEngine {
  private AgreementEffectivenessEngine() {}
  public static boolean isPreferentialEligible(TradeAgreement a, LocalDate asOf) {
    if (a == null || !a.isEnabled() || asOf == null) return false;
    AgreementStatus st;
    try { st = AgreementStatus.valueOf(a.getStatus()); }
    catch (Exception e) { return false; }
    if (!st.canPrefer()) return false;
    if (a.getEffectiveDate() != null && asOf.isBefore(a.getEffectiveDate())) return false;
    if (a.getExpiryDate() != null && asOf.isAfter(a.getExpiryDate())) return false;
    return true;
  }
}
