package com.automotive.costing.fta.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.fta.domain.OriginResult;
import com.automotive.costing.fta.dto.FtaDtos.*;
import com.automotive.costing.fta.engine.*;
import com.automotive.costing.fta.entity.*;
import com.automotive.costing.fta.repository.*;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Optional FTA analysis. Does not modify V279 DutyEngine / Normal Import.
 * Preferential result is independent; consumer may feed duty into import costing.
 */
@Service
@RequiredArgsConstructor
public class FtaAnalysisService {

  private final TradeAgreementRepository agreementRepository;
  private final TariffConcessionRepository concessionRepository;
  private final OriginRuleRepository originRuleRepository;
  private final OriginAssessmentRepository assessmentRepository;
  private final ProofOfOriginRepository proofRepository;
  private final TradeRemedyRuleRepository remedyRepository;
  private final TenantAccessGuard tenantAccessGuard;

  @Transactional
  public AnalyzeResponse analyze(AnalyzeRequest req) {
    String customerId = tenantAccessGuard.requireCustomerId();
    List<String> warnings = new ArrayList<>();
    if (req == null) throw new BusinessRuleViolationException("Request required", "FTA_REQ");
    if (req.hsCode() == null || req.hsCode().isBlank())
      throw new BusinessRuleViolationException("HS code required", "FTA_HS");
    if (req.originCountry() == null || req.destinationCountry() == null)
      throw new BusinessRuleViolationException("Origin and destination required", "FTA_COUNTRY");
    // Origin independent of export
    String export = req.exportCountry() != null ? req.exportCountry() : req.originCountry();
    LocalDate asOf = req.assessmentDate() != null ? req.assessmentDate() : LocalDate.of(1970, 1, 1);
    if (req.assessmentDate() == null) warnings.add("assessmentDate_defaulted");

    TradeAgreement agreement = null;
    if (req.agreementCode() != null) {
      agreement = agreementRepository.findByAgreementCodeAndVersionNo(req.agreementCode(), 1)
          .or(() -> agreementRepository.findAll().stream()
              .filter(a -> req.agreementCode().equalsIgnoreCase(a.getAgreementCode()))
              .findFirst())
          .orElse(null);
      if (agreement == null) warnings.add("agreement_not_found");
    } else {
      warnings.add("agreement_not_specified");
    }

    boolean prefOk = AgreementEffectivenessEngine.isPreferentialEligible(agreement, asOf);
    if (agreement != null && !prefOk) {
      warnings.add("agreement_not_in_force_or_outside_dates");
    }

    // Origin rule
    Optional<OriginRule> originRule = Optional.empty();
    if (agreement != null) {
      List<OriginRule> rules = originRuleRepository.findEffective(agreement.getAgreementId(), req.hsCode(), asOf);
      if (!rules.isEmpty()) originRule = Optional.of(rules.get(0));
      else warnings.add("origin_rule_missing");
    }
    boolean proofVerified = req.proofVerified();
    if (req.proofId() != null) {
      Optional<ProofOfOrigin> p = proofRepository.findByCustomerIdAndProofId(customerId, req.proofId());
      if (p.isEmpty()) {
        warnings.add("proof_not_found");
        proofVerified = false;
      } else if (!"VERIFIED".equalsIgnoreCase(p.get().getVerificationStatus())) {
        warnings.add("proof_not_verified");
        proofVerified = false;
      } else {
        proofVerified = true;
      }
    }

    OriginRuleEngine.OriginInputs inputs = new OriginRuleEngine.OriginInputs(
        req.originCountry(), export, req.finishedHs() != null ? req.finishedHs() : req.hsCode(),
        req.inputHs(), req.fob(), req.vnm(), proofVerified,
        req.thirdCountryMaterial(), req.substantialTransformation(), req.cumulationClaimed());
    OriginRuleEngine.OriginDecision origin = OriginRuleEngine.assess(originRule, inputs);

    // Tariff concession only if origin ELIGIBLE and agreement effective
    Optional<TariffConcession> concession = Optional.empty();
    if (prefOk && origin.result() == OriginResult.ELIGIBLE && agreement != null) {
      List<TariffConcession> list = concessionRepository.findEffective(
          agreement.getAgreementId(), req.hsCode(), req.originCountry(), req.destinationCountry(), asOf);
      if (!list.isEmpty()) concession = Optional.of(list.get(0));
      else warnings.add("tariff_concession_missing");
    }
    TariffConcessionEngine.DutyCompare duty = TariffConcessionEngine.compare(
        concession, req.customsValue(), req.quantity());

    // Trade remedies always independent
    List<TradeRemedyRule> remedies = remedyRepository.findEffective(req.hsCode(), req.originCountry(), asOf);
    List<TradeRemedyEngine.RemedyLine> remedyLines =
        TradeRemedyEngine.apply(remedies, req.customsValue(), req.quantity());

    String resultCode = origin.result().name();
    if (prefOk && origin.result() == OriginResult.ELIGIBLE && !duty.applied()) {
      resultCode = OriginResult.REVIEW.name();
      warnings.add("eligible_origin_but_no_concession");
    }

    Instant now = Instant.now();
    String id = UUID.randomUUID().toString();
    OriginAssessment a = new OriginAssessment();
    a.setAssessmentId(id);
    a.setCustomerId(customerId);
    a.setAgreementId(agreement != null ? agreement.getAgreementId() : null);
    a.setProductCode(req.productCode());
    a.setHsCode(req.hsCode());
    a.setOriginCountry(req.originCountry());
    a.setExportCountry(export);
    a.setDestinationCountry(req.destinationCountry());
    a.setAssessmentDate(asOf);
    a.setResultCode(resultCode);
    a.setReason(origin.reason());
    a.setRvcPct(origin.rvcPct());
    a.setClassificationResult(origin.classificationResult());
    a.setCumulationResult(origin.cumulationResult());
    a.setProofId(req.proofId());
    a.setRuleVersion(originRule.map(OriginRule::getVersionNo).orElse(null));
    a.setPreferentialRate(concession.map(TariffConcession::getPreferentialRate).orElse(null));
    a.setNormalRate(concession.map(TariffConcession::getNormalRate).orElse(null));
    a.setDutyBenefit(duty.benefit());
    a.setLifecycleStatus("DRAFT");
    a.setCreatedAt(now);
    a.setUpdatedAt(now);
    assessmentRepository.save(a);

    return new AnalyzeResponse(
        id, resultCode, origin.reason(), "DRAFT",
        agreement != null ? agreement.getAgreementCode() : null,
        agreement != null ? agreement.getStatus() : null,
        prefOk,
        a.getNormalRate(), a.getPreferentialRate(),
        duty.normalDuty(), duty.preferentialDuty(), duty.benefit(),
        origin.rvcPct(), origin.classificationResult(), origin.cumulationResult(),
        req.originCountry(), export, req.destinationCountry(),
        remedyLines.stream().map(r -> new RemedyResponse(r.measureType(), r.amount(), r.formula())).toList(),
        warnings);
  }

  @Transactional(readOnly = true)
  public AnalyzeResponse get(String id) {
    String customerId = tenantAccessGuard.requireCustomerId();
    OriginAssessment a = assessmentRepository.findByCustomerIdAndAssessmentId(customerId, id)
        .orElseThrow(() -> new BusinessRuleViolationException("Assessment not found", "FTA_NOT_FOUND"));
    return new AnalyzeResponse(
        a.getAssessmentId(), a.getResultCode(), a.getReason(), a.getLifecycleStatus(),
        null, null, false,
        a.getNormalRate(), a.getPreferentialRate(),
        null, null, a.getDutyBenefit(),
        a.getRvcPct(), a.getClassificationResult(), a.getCumulationResult(),
        a.getOriginCountry(), a.getExportCountry(), a.getDestinationCountry(),
        List.of(), List.of());
  }

  @Transactional
  public AnalyzeResponse transition(String id, String action, String actor) {
    String customerId = tenantAccessGuard.requireCustomerId();
    OriginAssessment a = assessmentRepository.findByCustomerIdAndAssessmentId(customerId, id)
        .orElseThrow(() -> new BusinessRuleViolationException("Assessment not found", "FTA_NOT_FOUND"));
    String act = action == null ? "" : action.toUpperCase();
    switch (act) {
      case "SUBMIT" -> {
        if (!"DRAFT".equals(a.getLifecycleStatus()))
          throw new BusinessRuleViolationException("Only DRAFT can submit", "FTA_TRANSITION");
        a.setLifecycleStatus("SUBMITTED");
      }
      case "APPROVE" -> {
        a.setLifecycleStatus("APPROVED");
        a.setApprovedBy(actor);
        a.setApprovedAt(Instant.now());
      }
      case "LOCK" -> {
        if (!"APPROVED".equals(a.getLifecycleStatus()))
          throw new BusinessRuleViolationException("Only APPROVED can lock", "FTA_TRANSITION");
        a.setLifecycleStatus("LOCKED");
        a.setLockedAt(Instant.now());
      }
      default -> throw new BusinessRuleViolationException("Unknown action", "FTA_ACTION");
    }
    a.setUpdatedAt(Instant.now());
    assessmentRepository.save(a);
    return get(id);
  }

  @Transactional(readOnly = true)
  public List<TradeAgreement> listAgreements() {
    tenantAccessGuard.requireCustomerId();
    return agreementRepository.findAll();
  }
}
