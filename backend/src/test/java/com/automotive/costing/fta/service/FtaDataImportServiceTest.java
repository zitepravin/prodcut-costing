package com.automotive.costing.fta.service;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.fta.service.FtaDataImportService.AgreementImportRow;
import com.automotive.costing.fta.service.FtaDataImportService.ConcessionImportRow;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class FtaDataImportServiceTest {
  @Test void rejects_missing_source() {
    assertThrows(BusinessRuleViolationException.class, () ->
        FtaDataImportService.validateAgreement(new AgreementImportRow(
            "AITIGA", "Name", "IN_FORCE", LocalDate.of(2020,1,1), null,
            null, "ref", 1, LocalDate.of(2026,1,1))));
  }
  @Test void rejects_missing_official_ref() {
    assertThrows(BusinessRuleViolationException.class, () ->
        FtaDataImportService.validateAgreement(new AgreementImportRow(
            "AITIGA", "Name", "IN_FORCE", LocalDate.of(2020,1,1), null,
            "gov.source", null, 1, null)));
  }
  @Test void accepts_complete_row() {
    assertDoesNotThrow(() ->
        FtaDataImportService.validateAgreement(new AgreementImportRow(
            "AITIGA", "ASEAN-India", "IN_FORCE", LocalDate.of(2010,1,1), null,
            "MOC India", "https://example.gov/aitiga", 1, LocalDate.of(2026,1,1))));
  }
  @Test void concession_requires_rate_from_source() {
    assertThrows(BusinessRuleViolationException.class, () ->
        FtaDataImportService.validateConcession(new ConcessionImportRow(
            "AITIGA", "7208", "VN", "IN", null, null, "PERCENTAGE",
            LocalDate.of(2020,1,1), null, "src", "ref", 1)));
  }
}
