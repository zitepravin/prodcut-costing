package com.automotive.costing.fta.service;
import com.automotive.costing.fta.dto.FtaDtos.AnalyzeRequest;
import com.automotive.costing.fta.entity.*;
import com.automotive.costing.fta.repository.*;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtaAnalysisServiceTest {
  @Mock TradeAgreementRepository agreementRepository;
  @Mock TariffConcessionRepository concessionRepository;
  @Mock OriginRuleRepository originRuleRepository;
  @Mock OriginAssessmentRepository assessmentRepository;
  @Mock ProofOfOriginRepository proofRepository;
  @Mock TradeRemedyRuleRepository remedyRepository;
  @Mock TenantAccessGuard tenantAccessGuard;
  FtaAnalysisService service;

  @BeforeEach void setUp() {
    service = new FtaAnalysisService(agreementRepository, concessionRepository, originRuleRepository,
        assessmentRepository, proofRepository, remedyRepository, tenantAccessGuard);
    lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-1");
    lenient().when(assessmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    lenient().when(remedyRepository.findEffective(any(), any(), any())).thenReturn(List.of());
  }

  @Test void g2_eligible_with_pref() {
    TradeAgreement a = inForce("AITIGA");
    when(agreementRepository.findByAgreementCodeAndVersionNo("AITIGA", 1)).thenReturn(Optional.of(a));
    OriginRule or = new OriginRule();
    or.setRuleType("RVC"); or.setRvcThresholdPct(new BigDecimal("35")); or.setVersionNo(1);
    or.setCumulationAllowed(true);
    when(originRuleRepository.findEffective(eq(a.getAgreementId()), any(), any())).thenReturn(List.of(or));
    TariffConcession tc = new TariffConcession();
    tc.setNormalRate(new BigDecimal("0.10")); tc.setPreferentialRate(BigDecimal.ZERO); tc.setRateType("PERCENTAGE");
    when(concessionRepository.findEffective(any(), any(), any(), any(), any())).thenReturn(List.of(tc));

    var resp = service.analyze(new AnalyzeRequest("AITIGA", "7208.51", "P1", "VN", "VN", "IN",
        LocalDate.of(2026,1,1), new BigDecimal("1000"), BigDecimal.TEN,
        new BigDecimal("100"), new BigDecimal("30"), "7208.51", "7208.51",
        false, true, false, null, true));
    assertEquals("ELIGIBLE", resp.resultCode());
    assertTrue(resp.agreementPreferentialEligible());
    assertNotNull(resp.dutyBenefit());
  }

  @Test void g3_signed_no_pref() {
    TradeAgreement a = new TradeAgreement();
    a.setAgreementId("a1"); a.setAgreementCode("IND-UK"); a.setStatus("SIGNED"); a.setEnabled(true);
    a.setEffectiveDate(LocalDate.of(2030,1,1));
    when(agreementRepository.findByAgreementCodeAndVersionNo("IND-UK", 1)).thenReturn(Optional.of(a));
    when(originRuleRepository.findEffective(any(), any(), any())).thenReturn(List.of());
    var resp = service.analyze(new AnalyzeRequest("IND-UK", "8471", null, "GB", "GB", "IN",
        LocalDate.of(2026,1,1), new BigDecimal("100"), BigDecimal.ONE,
        null, null, null, null, false, false, false, null, true));
    assertFalse(resp.agreementPreferentialEligible());
    assertTrue(resp.warnings().stream().anyMatch(w -> w.contains("not_in_force")));
  }

  @Test void origin_not_equal_export() {
    // Manufactured CN exported SG — origin remains CN
    TradeAgreement a = inForce("CECA-SG");
    when(agreementRepository.findByAgreementCodeAndVersionNo(any(), anyInt())).thenReturn(Optional.of(a));
    OriginRule or = new OriginRule();
    or.setRuleType("WHOLLY_OBTAINED");
    when(originRuleRepository.findEffective(any(), any(), any())).thenReturn(List.of(or));
    var resp = service.analyze(new AnalyzeRequest("CECA-SG", "8471", null, "CN", "SG", "IN",
        LocalDate.of(2026,1,1), BigDecimal.TEN, BigDecimal.ONE,
        null, null, null, null, true, false, false, null, true));
    assertEquals("CN", resp.originCountry());
    assertEquals("SG", resp.exportCountry());
    assertEquals("NOT_ELIGIBLE", resp.resultCode()); // third country material for wholly obtained
  }

  private static TradeAgreement inForce(String code) {
    TradeAgreement a = new TradeAgreement();
    a.setAgreementId("ag-" + code);
    a.setAgreementCode(code);
    a.setStatus("IN_FORCE");
    a.setEnabled(true);
    a.setEffectiveDate(LocalDate.of(2020,1,1));
    a.setExpiryDate(LocalDate.of(2035,1,1));
    a.setCreatedAt(Instant.now());
    a.setUpdatedAt(Instant.now());
    return a;
  }
}
