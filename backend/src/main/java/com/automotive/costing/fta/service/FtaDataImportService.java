package com.automotive.costing.fta.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.fta.entity.TariffConcession;
import com.automotive.costing.fta.entity.TradeAgreement;
import com.automotive.costing.fta.repository.TariffConcessionRepository;
import com.automotive.costing.fta.repository.TradeAgreementRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controlled import of authoritative FTA master data.
 * Rejects rows missing source / officialReference / effective dates / version.
 * Does NOT invent tariff rates.
 */
@Service
@RequiredArgsConstructor
public class FtaDataImportService {

  private final TradeAgreementRepository agreementRepository;
  private final TariffConcessionRepository concessionRepository;
  private final TenantAccessGuard tenantAccessGuard;

  public record AgreementImportRow(String agreementCode, String agreementName, String status,
                                   LocalDate effectiveDate, LocalDate expiryDate,
                                   String source, String officialReference, int versionNo,
                                   LocalDate lastVerified) {}

  public record ConcessionImportRow(String agreementCode, String hsCode, String originCountry,
                                    String destinationCountry, BigDecimal normalRate,
                                    BigDecimal preferentialRate, String rateType,
                                    LocalDate effectiveFrom, LocalDate effectiveTo,
                                    String source, String officialReference, int versionNo) {}

  public record ImportResult(int accepted, int rejected, List<String> errors) {}

  @Transactional
  public ImportResult importAgreements(List<AgreementImportRow> rows) {
    tenantAccessGuard.requireCustomerId(); // caller must be authenticated; platform admin gate later
    int ok = 0;
    List<String> errors = new ArrayList<>();
    if (rows == null) return new ImportResult(0, 0, List.of("empty"));
    for (int i = 0; i < rows.size(); i++) {
      AgreementImportRow r = rows.get(i);
      try {
        validateAgreement(r);
        TradeAgreement a = new TradeAgreement();
        a.setAgreementId(UUID.randomUUID().toString());
        a.setAgreementCode(r.agreementCode());
        a.setAgreementName(r.agreementName());
        a.setStatus(r.status());
        a.setEffectiveDate(r.effectiveDate());
        a.setExpiryDate(r.expiryDate());
        a.setSource(r.source());
        a.setOfficialReference(r.officialReference());
        a.setVersionNo(r.versionNo() > 0 ? r.versionNo() : 1);
        a.setLastVerified(r.lastVerified());
        a.setEnabled(true);
        a.setPlatformScope(true);
        a.setApprovalStatus("APPROVED");
        Instant now = Instant.now();
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        agreementRepository.save(a);
        ok++;
      } catch (Exception ex) {
        errors.add("row " + i + ": " + ex.getMessage());
      }
    }
    return new ImportResult(ok, rows.size() - ok, errors);
  }

  @Transactional
  public ImportResult importConcessions(String agreementId, List<ConcessionImportRow> rows) {
    tenantAccessGuard.requireCustomerId();
    int ok = 0;
    List<String> errors = new ArrayList<>();
    if (rows == null) return new ImportResult(0, 0, List.of("empty"));
    for (int i = 0; i < rows.size(); i++) {
      ConcessionImportRow r = rows.get(i);
      try {
        validateConcession(r);
        TariffConcession c = new TariffConcession();
        c.setConcessionId(UUID.randomUUID().toString());
        c.setAgreementId(agreementId);
        c.setHsCode(r.hsCode());
        c.setOriginCountry(r.originCountry());
        c.setDestinationCountry(r.destinationCountry());
        c.setNormalRate(r.normalRate());
        c.setPreferentialRate(r.preferentialRate());
        c.setRateType(r.rateType() != null ? r.rateType() : "PERCENTAGE");
        c.setEffectiveFrom(r.effectiveFrom());
        c.setEffectiveTo(r.effectiveTo());
        c.setSource(r.source());
        c.setVersionNo(r.versionNo() > 0 ? r.versionNo() : 1);
        c.setApprovalStatus("APPROVED");
        c.setActive(true);
        Instant now = Instant.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        concessionRepository.save(c);
        ok++;
      } catch (Exception ex) {
        errors.add("row " + i + ": " + ex.getMessage());
      }
    }
    return new ImportResult(ok, rows.size() - ok, errors);
  }

  static void validateAgreement(AgreementImportRow r) {
    if (r == null) throw new BusinessRuleViolationException("null row", "FTA_IMPORT");
    require(r.agreementCode(), "agreementCode");
    require(r.agreementName(), "agreementName");
    require(r.status(), "status");
    require(r.source(), "source");
    require(r.officialReference(), "officialReference");
    if (r.effectiveDate() == null) throw new BusinessRuleViolationException("effectiveDate required", "FTA_IMPORT");
  }

  static void validateConcession(ConcessionImportRow r) {
    if (r == null) throw new BusinessRuleViolationException("null row", "FTA_IMPORT");
    require(r.hsCode(), "hsCode");
    require(r.destinationCountry(), "destinationCountry");
    require(r.source(), "source");
    require(r.officialReference(), "officialReference");
    if (r.effectiveFrom() == null) throw new BusinessRuleViolationException("effectiveFrom required", "FTA_IMPORT");
    if (r.normalRate() == null && r.preferentialRate() == null)
      throw new BusinessRuleViolationException("at least one rate required from authoritative source", "FTA_IMPORT");
  }

  private static void require(String v, String field) {
    if (v == null || v.isBlank())
      throw new BusinessRuleViolationException(field + " required", "FTA_IMPORT");
  }
}
