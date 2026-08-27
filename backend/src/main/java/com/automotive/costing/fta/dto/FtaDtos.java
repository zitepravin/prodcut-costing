package com.automotive.costing.fta.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public final class FtaDtos {
  private FtaDtos() {}
  public record AnalyzeRequest(
      String agreementCode, String hsCode, String productCode,
      String originCountry, String exportCountry, String destinationCountry,
      LocalDate assessmentDate, BigDecimal customsValue, BigDecimal quantity,
      BigDecimal fob, BigDecimal vnm, String inputHs, String finishedHs,
      boolean thirdCountryMaterial, boolean substantialTransformation,
      boolean cumulationClaimed, String proofId, boolean proofVerified
  ) {}
  public record RemedyResponse(String measureType, BigDecimal amount, String formula) {}
  public record AnalyzeResponse(
      String assessmentId, String resultCode, String reason, String lifecycleStatus,
      String agreementCode, String agreementStatus, boolean agreementPreferentialEligible,
      BigDecimal normalRate, BigDecimal preferentialRate,
      BigDecimal normalDuty, BigDecimal preferentialDuty, BigDecimal dutyBenefit,
      BigDecimal rvcPct, String classificationResult, String cumulationResult,
      String originCountry, String exportCountry, String destinationCountry,
      List<RemedyResponse> remedies, List<String> warnings
  ) {}
}
