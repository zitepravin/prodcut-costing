package com.automotive.costing.importcosting.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.importcosting.entity.ImportDutyRule;
import com.automotive.costing.importcosting.entity.ImportTaxRule;
import com.automotive.costing.importcosting.repository.ImportDutyRuleRepository;
import com.automotive.costing.importcosting.repository.ImportTaxRuleRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportConfigService {

  private final ImportDutyRuleRepository dutyRuleRepository;
  private final ImportTaxRuleRepository taxRuleRepository;
  private final TenantAccessGuard tenantAccessGuard;

  public record DutyRuleRequest(String hsCode, String originCountry, String destinationCountry,
                                String dutyType, BigDecimal adValoremRate, BigDecimal specificAmount,
                                String specificCurrency, String taxableBase,
                                LocalDate effectiveFrom, LocalDate effectiveTo, String source) {}
  public record TaxRuleRequest(String taxCode, String taxType, BigDecimal rate, String taxBase,
                               String jurisdiction, String destinationCountry, boolean recoverable,
                               boolean includedInLanded, LocalDate effectiveFrom, LocalDate effectiveTo, String source) {}

  @Transactional
  public ImportDutyRule createDuty(DutyRuleRequest req) {
    String customerId = tenantAccessGuard.requireCustomerId();
    if (req.hsCode() == null || req.originCountry() == null || req.destinationCountry() == null)
      throw new BusinessRuleViolationException("HS/origin/destination required", "DUTY_REQ");
    if (req.effectiveFrom() == null)
      throw new BusinessRuleViolationException("effectiveFrom required", "DUTY_EFF");
    ImportDutyRule r = new ImportDutyRule();
    r.setDutyRuleId(UUID.randomUUID().toString());
    r.setCustomerId(customerId);
    r.setHsCode(req.hsCode());
    r.setOriginCountry(req.originCountry());
    r.setDestinationCountry(req.destinationCountry());
    r.setDutyType(req.dutyType() != null ? req.dutyType() : "AD_VALOREM");
    r.setAdValoremRate(req.adValoremRate());
    r.setSpecificAmount(req.specificAmount());
    r.setSpecificCurrency(req.specificCurrency());
    r.setTaxableBase(req.taxableBase() != null ? req.taxableBase() : "CUSTOMS_VALUE");
    r.setEffectiveFrom(req.effectiveFrom());
    r.setEffectiveTo(req.effectiveTo());
    r.setSource(req.source());
    r.setVersionNo(1);
    r.setApprovalStatus("DRAFT");
    r.setActive(true);
    Instant now = Instant.now();
    r.setCreatedAt(now);
    r.setUpdatedAt(now);
    return dutyRuleRepository.save(r);
  }

  @Transactional
  public ImportDutyRule approveDuty(String id) {
    String customerId = tenantAccessGuard.requireCustomerId();
    ImportDutyRule r = dutyRuleRepository.findById(id)
        .filter(x -> customerId.equals(x.getCustomerId()))
        .orElseThrow(() -> new BusinessRuleViolationException("Duty rule not found", "DUTY_NOT_FOUND"));
    if ("APPROVED".equals(r.getApprovalStatus()))
      throw new BusinessRuleViolationException("Already approved", "DUTY_IMMUTABLE");
    r.setApprovalStatus("APPROVED");
    r.setUpdatedAt(Instant.now());
    return dutyRuleRepository.save(r);
  }

  @Transactional
  public ImportTaxRule createTax(TaxRuleRequest req) {
    String customerId = tenantAccessGuard.requireCustomerId();
    if (req.taxCode() == null || req.rate() == null || req.effectiveFrom() == null)
      throw new BusinessRuleViolationException("taxCode/rate/effectiveFrom required", "TAX_REQ");
    ImportTaxRule r = new ImportTaxRule();
    r.setTaxRuleId(UUID.randomUUID().toString());
    r.setCustomerId(customerId);
    r.setTaxCode(req.taxCode());
    r.setTaxType(req.taxType() != null ? req.taxType() : req.taxCode());
    r.setRate(req.rate());
    r.setTaxBase(req.taxBase() != null ? req.taxBase() : "CUSTOMS_VALUE_PLUS_DUTY");
    r.setJurisdiction(req.jurisdiction());
    r.setDestinationCountry(req.destinationCountry());
    r.setRecoverable(req.recoverable());
    r.setIncludedInLanded(req.includedInLanded());
    r.setEffectiveFrom(req.effectiveFrom());
    r.setEffectiveTo(req.effectiveTo());
    r.setSource(req.source());
    r.setVersionNo(1);
    r.setApprovalStatus("DRAFT");
    r.setActive(true);
    Instant now = Instant.now();
    r.setCreatedAt(now);
    r.setUpdatedAt(now);
    return taxRuleRepository.save(r);
  }

  @Transactional
  public ImportTaxRule approveTax(String id) {
    String customerId = tenantAccessGuard.requireCustomerId();
    ImportTaxRule r = taxRuleRepository.findById(id)
        .filter(x -> customerId.equals(x.getCustomerId()))
        .orElseThrow(() -> new BusinessRuleViolationException("Tax rule not found", "TAX_NOT_FOUND"));
    if ("APPROVED".equals(r.getApprovalStatus()))
      throw new BusinessRuleViolationException("Already approved", "TAX_IMMUTABLE");
    r.setApprovalStatus("APPROVED");
    r.setUpdatedAt(Instant.now());
    return taxRuleRepository.save(r);
  }
}
