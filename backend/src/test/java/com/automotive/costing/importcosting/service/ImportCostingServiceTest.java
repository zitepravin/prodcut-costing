package com.automotive.costing.importcosting.service;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertRequest;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertResponse;
import com.automotive.costing.currency.service.CurrencyService;
import com.automotive.costing.importcosting.dto.ImportCostingDtos.*;
import com.automotive.costing.importcosting.entity.ImportCostCalculation;
import com.automotive.costing.importcosting.entity.ImportDutyRule;
import com.automotive.costing.importcosting.entity.ImportTaxRule;
import com.automotive.costing.importcosting.repository.*;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportCostingServiceTest {
  private static final String TENANT = "cust-1";
  @Mock ImportCostCalculationRepository calculationRepository;
  @Mock ImportCostComponentRepository componentRepository;
  @Mock ImportDutyRuleRepository dutyRuleRepository;
  @Mock ImportTaxRuleRepository taxRuleRepository;
  @Mock ImportShipmentRepository shipmentRepository;
  @Mock ImportShipmentItemRepository shipmentItemRepository;
  @Mock TenantAccessGuard tenantAccessGuard;
  @Mock CurrencyService currencyService;
  ImportCostingService service;

  @BeforeEach void setUp() {
    service = new ImportCostingService(calculationRepository, componentRepository,
        dutyRuleRepository, taxRuleRepository, shipmentRepository, shipmentItemRepository,
        tenantAccessGuard, currencyService);
    lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn(TENANT);
    lenient().when(calculationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    lenient().when(componentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    lenient().when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    lenient().when(shipmentItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test void g1_domestic_missing_duty_marked() {
    when(dutyRuleRepository.findEffective(any(), any(), any(), any(), any())).thenReturn(List.of());
    when(taxRuleRepository.findEffective(any(), any(), any())).thenReturn(List.of());
    var resp = service.calculate(baseReq("IN", "IN", "IN", "9999", "100", null, null));
    assertEquals("MISSING_DATA", resp.reviewStatus());
  }

  @Test void g2_china_to_india_normal_import() {
    ImportDutyRule duty = new ImportDutyRule();
    duty.setDutyType("AD_VALOREM");
    duty.setAdValoremRate(new BigDecimal("0.10"));
    when(dutyRuleRepository.findEffective(eq(TENANT), eq("8471.30"), eq("CN"), eq("IN"), any()))
        .thenReturn(List.of(duty));
    ImportTaxRule tax = new ImportTaxRule();
    tax.setTaxCode("IGST"); tax.setTaxType("GST"); tax.setRate(new BigDecimal("0.18"));
    tax.setTaxBase("CUSTOMS_VALUE_PLUS_DUTY"); tax.setRecoverable(true); tax.setIncludedInLanded(false);
    when(taxRuleRepository.findEffective(eq(TENANT), eq("IN"), any())).thenReturn(List.of(tax));
    var resp = service.calculate(baseReq("CN", "CN", "IN", "8471.30", "1000",
        new BigDecimal("50"), new BigDecimal("10")));
    assertEquals("OK", resp.reviewStatus());
    assertEquals(0, new BigDecimal("1160.000000").compareTo(resp.totalLandedCost()));
  }

  @Test void g4_multi_item_shipment_allocation() {
    when(dutyRuleRepository.findEffective(any(), any(), any(), any(), any())).thenReturn(List.of());
    when(taxRuleRepository.findEffective(any(), any(), any())).thenReturn(List.of());
    var lines = List.of(
        new LineInput("m1", "P1", "7208", new BigDecimal("5"), "EA",
            new BigDecimal("10"), new BigDecimal("12"), null, new BigDecimal("20"), "USD", new BigDecimal("100")),
        new LineInput("m2", "P2", "7208", new BigDecimal("5"), "EA",
            new BigDecimal("10"), new BigDecimal("12"), null, new BigDecimal("20"), "USD", new BigDecimal("100"))
    );
    var req = new CalculateRequest(
        "sup", "CN", "CN", "CN", "IN", null, null, "7208",
        null, null, null, null, null, "USD", null, null,
        "FOB", "SEA", LocalDate.of(2026,1,1), LocalDate.of(2026,1,5),
        LocalDate.of(2026,1,5), LocalDate.of(2026,1,5), "USD",
        new BigDecimal("40"), null, null, null, null, null, null, null, null,
        lines, null, "VALUE", null);
    var resp = service.calculate(req);
    assertNotNull(resp.calculationId());
    assertEquals(0, new BigDecimal("240.000000").compareTo(resp.totalLandedCost())); // 200 + 40 freight
    verify(shipmentItemRepository, times(2)).save(any());
  }

  @Test void g5_fx_conversion_called() {
    when(dutyRuleRepository.findEffective(any(), any(), any(), any(), any())).thenReturn(List.of());
    when(taxRuleRepository.findEffective(any(), any(), any())).thenReturn(List.of());
    when(currencyService.convert(any())).thenReturn(new ConvertResponse(
        new BigDecimal("100"), "USD", new BigDecimal("8300"), "INR",
        new BigDecimal("83"), LocalDate.of(2026,1,5), "DIRECT", "RBI"));
    var req = new CalculateRequest(
        "sup", "US", "US", "US", "IN", "m1", "P1", "8471",
        new BigDecimal("1"), "EA", null, null, null, "USD", new BigDecimal("100"), new BigDecimal("100"),
        "FOB", "AIR", LocalDate.of(2026,1,1), LocalDate.of(2026,1,5),
        LocalDate.of(2026,1,5), LocalDate.of(2026,1,5), "INR",
        null, null, null, null, null, null, null, null, null, null, null, null, null);
    var resp = service.calculate(req);
    assertEquals(0, new BigDecimal("8300.000000").compareTo(resp.invoiceValue()));
    verify(currencyService).convert(any(ConvertRequest.class));
  }

  @Test void g6_duty_plus_tax() {
    ImportDutyRule duty = new ImportDutyRule();
    duty.setDutyType("AD_VALOREM");
    duty.setAdValoremRate(new BigDecimal("0.05"));
    when(dutyRuleRepository.findEffective(any(), any(), any(), any(), any())).thenReturn(List.of(duty));
    ImportTaxRule tax = new ImportTaxRule();
    tax.setTaxCode("VAT"); tax.setTaxType("VAT"); tax.setRate(new BigDecimal("0.20"));
    tax.setTaxBase("CUSTOMS_VALUE_PLUS_DUTY"); tax.setRecoverable(false); tax.setIncludedInLanded(true);
    when(taxRuleRepository.findEffective(any(), any(), any())).thenReturn(List.of(tax));
    var resp = service.calculate(baseReq("VN", "VN", "IN", "7208", "200", null, null));
    assertEquals(0, new BigDecimal("252.000000").compareTo(resp.totalLandedCost()));
  }

  @Test void negative_quantity_rejected() {
    assertThrows(BusinessRuleViolationException.class, () ->
        service.calculate(new CalculateRequest(
            null, null, "CN", "CN", "IN", null, null, "8471",
            new BigDecimal("-1"), "EA", null, null, null, "USD",
            BigDecimal.ONE, BigDecimal.ONE, "FOB", "SEA",
            LocalDate.of(2026,1,1), LocalDate.of(2026,1,5), LocalDate.of(2026,1,5), LocalDate.of(2026,1,5),
            "USD", null, null, null, null, null, null, null, null, null, null, null, null, null)));
  }

  @Test void lifecycle_freeze() {
    when(dutyRuleRepository.findEffective(any(), any(), any(), any(), any())).thenReturn(List.of());
    when(taxRuleRepository.findEffective(any(), any(), any())).thenReturn(List.of());
    AtomicReference<ImportCostCalculation> store = new AtomicReference<>();
    when(calculationRepository.save(any())).thenAnswer(inv -> { store.set(inv.getArgument(0)); return inv.getArgument(0); });
    when(calculationRepository.findByCustomerIdAndCalculationId(eq(TENANT), anyString()))
        .thenAnswer(inv -> Optional.ofNullable(store.get()));
    when(componentRepository.findByCustomerIdAndCalculationIdOrderBySortOrderAsc(any(), any())).thenReturn(List.of());
    var created = service.calculate(baseReq("CN", "CN", "IN", "1000", "100", null, null));
    assertEquals("APPROVED", service.transition(created.calculationId(), new LifecycleRequest("APPROVE", "u")).lifecycleStatus());
    assertEquals("LOCKED", service.transition(created.calculationId(), new LifecycleRequest("LOCK", "u")).lifecycleStatus());
  }

  private static CalculateRequest baseReq(String origin, String export, String dest, String hs,
                                          String invoice, BigDecimal freight, BigDecimal insurance) {
    return new CalculateRequest(
        "sup-1", origin, origin, export, dest, "mat-1", "P1", hs,
        new BigDecimal("10"), "EA", new BigDecimal("5"), new BigDecimal("6"), null,
        "USD", null, new BigDecimal(invoice), "FOB", "SEA",
        LocalDate.of(2026,1,1), LocalDate.of(2026,1,5), LocalDate.of(2026,1,5), LocalDate.of(2026,1,5),
        "USD", freight, null, insurance, null, null, null, null, null, null, null, null, "VALUE", null);
  }
}
