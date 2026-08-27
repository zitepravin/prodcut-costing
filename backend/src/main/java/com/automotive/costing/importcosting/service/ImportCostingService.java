package com.automotive.costing.importcosting.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertRequest;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertResponse;
import com.automotive.costing.currency.service.CurrencyService;
import com.automotive.costing.importcosting.domain.ImportLifecycleStatus;
import com.automotive.costing.importcosting.dto.ImportCostingDtos.*;
import com.automotive.costing.importcosting.engine.*;
import com.automotive.costing.importcosting.entity.*;
import com.automotive.costing.importcosting.repository.*;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImportCostingService {

  private static final int SCALE = 6;

  private final ImportCostCalculationRepository calculationRepository;
  private final ImportCostComponentRepository componentRepository;
  private final ImportDutyRuleRepository dutyRuleRepository;
  private final ImportTaxRuleRepository taxRuleRepository;
  private final ImportShipmentRepository shipmentRepository;
  private final ImportShipmentItemRepository shipmentItemRepository;
  private final TenantAccessGuard tenantAccessGuard;
  private final CurrencyService currencyService;

  @Transactional
  public CalculationResponse calculate(CalculateRequest req) {
    String customerId = tenantAccessGuard.requireCustomerId();
    List<String> missing = new ArrayList<>();
    validateInputs(req, missing);

    LocalDate costingDate = req.costingDate() != null ? req.costingDate() : req.customsDate();
    LocalDate fxAsOf = req.fxAsOfDate() != null ? req.fxAsOfDate() : costingDate;
    if (costingDate == null) missing.add("costingDate");
    if (fxAsOf == null) missing.add("fxAsOfDate");

    String targetCcy = req.targetCurrency() != null ? req.targetCurrency() : req.invoiceCurrency();
    if (targetCcy == null || targetCcy.isBlank()) missing.add("currency");

    // Multi-item path
    if (req.lines() != null && !req.lines().isEmpty()) {
      return calculateMultiItem(customerId, req, costingDate, fxAsOf, targetCcy, missing);
    }

    BigDecimal qty = req.quantity();
    BigDecimal invoiceValue = req.invoiceValue();
    if (invoiceValue == null && req.unitPurchasePrice() != null && qty != null) {
      invoiceValue = req.unitPurchasePrice().multiply(qty).setScale(SCALE, RoundingMode.HALF_UP);
    }
    if (invoiceValue == null) missing.add("invoiceValue");

    String sourceCcy = req.invoiceCurrency() != null ? req.invoiceCurrency() : targetCcy;
    FxSnap fxSnap = convertAmount(invoiceValue, sourceCcy, targetCcy, fxAsOf, missing);
    BigDecimal invoiceInTarget = fxSnap.amount();
    BigDecimal customsValue = invoiceInTarget;

    return persistSingleLine(customerId, req, costingDate, fxAsOf, targetCcy, qty,
        invoiceInTarget, customsValue, fxSnap, missing);
  }

  private CalculationResponse calculateMultiItem(String customerId, CalculateRequest req,
      LocalDate costingDate, LocalDate fxAsOf, String targetCcy, List<String> missing) {
    String method = req.allocationMethod() != null ? req.allocationMethod() : "VALUE";
    List<AllocationEngine.LineDriver> drivers = new ArrayList<>();
    BigDecimal totalInvoice = BigDecimal.ZERO;
    BigDecimal totalQty = BigDecimal.ZERO;
    String sourceCcy = req.invoiceCurrency() != null ? req.invoiceCurrency() : targetCcy;

    Instant now = Instant.now();
    String shipmentId = UUID.randomUUID().toString();
    ImportShipment ship = new ImportShipment();
    ship.setShipmentId(shipmentId);
    ship.setCustomerId(customerId);
    ship.setShipmentRef("SHP-" + shipmentId.substring(0, 8));
    ship.setSupplierId(req.supplierId());
    ship.setSupplierCountry(req.supplierCountry());
    ship.setOriginCountry(req.originCountry());
    ship.setExportCountry(req.exportCountry());
    ship.setDestinationCountry(req.destinationCountry());
    ship.setShipmentMode(req.shipmentMode());
    ship.setIncotermCode(req.incoterm());
    ship.setShipmentDate(req.shipmentDate());
    ship.setCustomsDate(req.customsDate());
    ship.setCurrencyCode(targetCcy != null ? targetCcy : "USD");
    ship.setStatus("DRAFT");
    ship.setCreatedAt(now);
    ship.setUpdatedAt(now);
    shipmentRepository.save(ship);

    int lineNo = 1;
    List<ImportShipmentItem> items = new ArrayList<>();
    for (LineInput li : req.lines()) {
      BigDecimal inv = li.invoiceValue();
      if (inv == null && li.unitPrice() != null && li.quantity() != null) {
        inv = li.unitPrice().multiply(li.quantity()).setScale(SCALE, RoundingMode.HALF_UP);
      }
      if (inv == null) { missing.add("lineInvoiceValue"); continue; }
      String lineCcy = li.invoiceCurrency() != null ? li.invoiceCurrency() : sourceCcy;
      FxSnap fx = convertAmount(inv, lineCcy, targetCcy, fxAsOf, missing);
      BigDecimal invT = fx.amount() != null ? fx.amount() : inv;

      ImportShipmentItem item = new ImportShipmentItem();
      item.setItemId(UUID.randomUUID().toString());
      item.setShipmentId(shipmentId);
      item.setCustomerId(customerId);
      item.setLineNo(lineNo++);
      item.setMaterialId(li.materialId());
      item.setProductCode(li.productCode());
      item.setHsCode(li.hsCode() != null ? li.hsCode() : req.hsCode());
      item.setQuantity(li.quantity());
      item.setUom(li.uom());
      item.setNetWeight(li.netWeight());
      item.setGrossWeight(li.grossWeight());
      item.setVolume(li.volume());
      item.setUnitPrice(li.unitPrice() != null ? li.unitPrice() : BigDecimal.ZERO);
      item.setInvoiceCurrency(lineCcy != null ? lineCcy : "USD");
      item.setInvoiceValue(invT);
      item.setCreatedAt(now);
      shipmentItemRepository.save(item);
      items.add(item);

      totalInvoice = totalInvoice.add(invT);
      if (li.quantity() != null) totalQty = totalQty.add(li.quantity());
      drivers.add(new AllocationEngine.LineDriver(item.getItemId(), invT, li.quantity(),
          li.netWeight(), li.grossWeight(), li.volume(), li.grossWeight()));
    }

    // Allocate shipment-level charges
    Map<String, BigDecimal> allocatedFreight = allocateCharge(req.internationalFreight(), method, drivers);
    Map<String, BigDecimal> allocatedInsurance = allocateCharge(req.insurance(), method, drivers);
    Map<String, BigDecimal> allocatedPort = allocateCharge(req.portCharges(), method, drivers);

    // Aggregate first-line calculation as shipment summary (representative)
    String hs = items.isEmpty() ? req.hsCode() : items.get(0).getHsCode();
    Optional<ImportDutyRule> dutyRule = Optional.empty();
    if (hs != null && req.originCountry() != null && req.destinationCountry() != null && costingDate != null) {
      List<ImportDutyRule> rules = dutyRuleRepository.findEffective(
          customerId, hs, req.originCountry(), req.destinationCountry(), costingDate);
      if (!rules.isEmpty()) dutyRule = Optional.of(rules.get(0));
      else missing.add("dutyRule");
    }
    DutyEngine.DutyResult duty = DutyEngine.calculate(dutyRule, totalInvoice, totalQty);
    if (duty.missingRule()) missing.add("dutyAmount");

    List<ImportTaxRule> taxRules = costingDate != null && req.destinationCountry() != null
        ? taxRuleRepository.findEffective(customerId, req.destinationCountry(), costingDate) : List.of();
    List<TaxEngine.TaxLine> taxLines = TaxEngine.calculate(taxRules, totalInvoice, duty.dutyAmount());

    List<LandedCostEngine.ComponentLine> comps = new ArrayList<>();
    comps.add(new LandedCostEngine.ComponentLine("PURCHASE_VALUE", totalInvoice, false, true, "sum line invoices"));
    if (req.internationalFreight() != null) {
      comps.add(new LandedCostEngine.ComponentLine("INTERNATIONAL_FREIGHT", req.internationalFreight(), false, true,
          "allocated by " + method));
    }
    if (req.insurance() != null) {
      comps.add(new LandedCostEngine.ComponentLine("INSURANCE", req.insurance(), false, true, "allocated by " + method));
    }
    if (duty.dutyAmount() != null) {
      comps.add(new LandedCostEngine.ComponentLine("CUSTOMS_DUTY", duty.dutyAmount(), false, true, duty.formulaNote()));
    }
    for (TaxEngine.TaxLine t : taxLines) {
      String code = t.recoverable() ? "RECOVERABLE_TAX" : "NON_RECOVERABLE_TAX";
      comps.add(new LandedCostEngine.ComponentLine(code, t.amount(), t.recoverable(), t.includedInLanded(),
          t.taxCode() + ": " + t.formulaNote()));
    }
    if (req.portCharges() != null) {
      comps.add(new LandedCostEngine.ComponentLine("PORT_CHARGES", req.portCharges(), false, true, "allocated"));
    }

    // Reconciliation: allocated freight sums to source
    if (req.internationalFreight() != null) {
      BigDecimal sumAlloc = allocatedFreight.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
      if (sumAlloc.subtract(req.internationalFreight()).abs().compareTo(new BigDecimal("0.01")) > 0) {
        missing.add("allocationReconciliation");
      }
    }

    LandedCostEngine.LandedResult landed = LandedCostEngine.build(totalInvoice, totalInvoice, totalQty, comps);
    return saveCalculation(customerId, shipmentId, null, costingDate, fxAsOf, targetCcy, landed, missing, req, comps);
  }

  private Map<String, BigDecimal> allocateCharge(BigDecimal charge, String method,
                                                  List<AllocationEngine.LineDriver> drivers) {
    Map<String, BigDecimal> map = new LinkedHashMap<>();
    if (charge == null || drivers.isEmpty()) return map;
    for (AllocationEngine.AllocationLine al : AllocationEngine.allocate(charge, method, drivers)) {
      map.put(al.itemId(), al.allocatedAmount());
    }
    return map;
  }

  private CalculationResponse persistSingleLine(String customerId, CalculateRequest req,
      LocalDate costingDate, LocalDate fxAsOf, String targetCcy, BigDecimal qty,
      BigDecimal invoiceInTarget, BigDecimal customsValue, FxSnap fxSnap, List<String> missing) {

    Optional<ImportDutyRule> dutyRule = Optional.empty();
    if (req.hsCode() != null && req.originCountry() != null && req.destinationCountry() != null && costingDate != null) {
      List<ImportDutyRule> rules = dutyRuleRepository.findEffective(
          customerId, req.hsCode(), req.originCountry(), req.destinationCountry(), costingDate);
      if (!rules.isEmpty()) dutyRule = Optional.of(rules.get(0));
      else missing.add("dutyRule");
    } else {
      missing.add("dutyRuleInputs");
    }
    DutyEngine.DutyResult duty = DutyEngine.calculate(dutyRule, customsValue, qty);
    if (duty.missingRule()) missing.add("dutyAmount");

    List<ImportTaxRule> taxRules = List.of();
    if (req.destinationCountry() != null && costingDate != null) {
      taxRules = taxRuleRepository.findEffective(customerId, req.destinationCountry(), costingDate);
    }
    List<TaxEngine.TaxLine> taxLines = TaxEngine.calculate(taxRules, customsValue, duty.dutyAmount());

    List<LandedCostEngine.ComponentLine> comps = new ArrayList<>();
    if (invoiceInTarget != null) {
      comps.add(new LandedCostEngine.ComponentLine("PURCHASE_VALUE", invoiceInTarget, false, true,
          fxSnap.note()));
    }
    addCharge(comps, "INTERNATIONAL_FREIGHT", req.internationalFreight());
    addCharge(comps, "INLAND_FREIGHT", req.inlandFreight());
    addCharge(comps, "INSURANCE", req.insurance());
    addCharge(comps, "PACKING", req.packing());
    addCharge(comps, "ORIGIN_CHARGES", req.originCharges());
    if (duty.dutyAmount() != null) {
      comps.add(new LandedCostEngine.ComponentLine("CUSTOMS_DUTY", duty.dutyAmount(), false, true, duty.formulaNote()));
    }
    for (TaxEngine.TaxLine t : taxLines) {
      String code = t.recoverable() ? "RECOVERABLE_TAX" : "NON_RECOVERABLE_TAX";
      comps.add(new LandedCostEngine.ComponentLine(code, t.amount(), t.recoverable(), t.includedInLanded(),
          t.taxCode() + ": " + t.formulaNote()));
    }
    addCharge(comps, "PORT_CHARGES", req.portCharges());
    addCharge(comps, "CUSTOMS_CLEARANCE", req.customsClearance());
    addCharge(comps, "BROKERAGE", req.brokerage());
    addCharge(comps, "OTHER_IMPORT_COST", req.otherImportCost());
    if (req.extraCharges() != null) {
      for (ChargeInput c : req.extraCharges()) {
        if (c.amount() != null) {
          comps.add(new LandedCostEngine.ComponentLine(
              c.componentCode() != null ? c.componentCode() : "OTHER_IMPORT_COST",
              c.amount(), false, true, "extra"));
        }
      }
    }

    LandedCostEngine.LandedResult landed = LandedCostEngine.build(invoiceInTarget, customsValue, qty, comps);
    return saveCalculation(customerId, null, null, costingDate, fxAsOf, targetCcy, landed, missing, req, comps);
  }

  private CalculationResponse saveCalculation(String customerId, String shipmentId, String itemId,
      LocalDate costingDate, LocalDate fxAsOf, String targetCcy, LandedCostEngine.LandedResult landed,
      List<String> missing, CalculateRequest req, List<LandedCostEngine.ComponentLine> comps) {
    String review = missing.isEmpty() ? "OK" : "MISSING_DATA";
    Instant now = Instant.now();
    String calcId = UUID.randomUUID().toString();
    ImportCostCalculation calc = new ImportCostCalculation();
    calc.setCalculationId(calcId);
    calc.setCustomerId(customerId);
    calc.setShipmentId(shipmentId);
    calc.setItemId(itemId);
    calc.setCostingDate(costingDate != null ? costingDate : LocalDate.of(1970, 1, 1));
    calc.setFxAsOfDate(fxAsOf != null ? fxAsOf : calc.getCostingDate());
    calc.setCurrencyCode(targetCcy != null ? targetCcy : "XXX");
    calc.setInvoiceValue(landed.invoiceValue());
    calc.setCustomsValue(landed.customsValue());
    calc.setTotalLandedCost(landed.totalLanded());
    calc.setUnitLandedCost(landed.unitLanded());
    calc.setQuantity(landed.quantity());
    calc.setLifecycleStatus(ImportLifecycleStatus.DRAFT.name());
    calc.setReviewStatus(review);
    calc.setMissingDataJson(missing.isEmpty() ? null : toJsonArray(missing));
    calc.setIndustryProfile(req.industryProfile());
    calc.setCreatedAt(now);
    calc.setUpdatedAt(now);
    calc.setVersionNo(1);
    calculationRepository.save(calc);

    int order = 0;
    List<ComponentResponse> respComps = new ArrayList<>();
    for (LandedCostEngine.ComponentLine c : comps) {
      ImportCostComponent e = new ImportCostComponent();
      e.setComponentId(UUID.randomUUID().toString());
      e.setCalculationId(calcId);
      e.setCustomerId(customerId);
      e.setComponentCode(c.code());
      e.setAmount(c.amount() != null ? c.amount() : BigDecimal.ZERO);
      e.setCurrencyCode(calc.getCurrencyCode());
      e.setRecoverable(c.recoverable());
      e.setIncludedInLanded(c.includedInLanded());
      e.setFormulaNote(c.formulaNote());
      e.setSortOrder(order++);
      componentRepository.save(e);
      respComps.add(new ComponentResponse(c.code(), c.amount(), calc.getCurrencyCode(),
          c.recoverable(), c.includedInLanded(), null, null, c.formulaNote()));
    }
    return new CalculationResponse(calc.getCalculationId(), calc.getLifecycleStatus(), calc.getReviewStatus(),
        calc.getCurrencyCode(), calc.getCostingDate(), calc.getFxAsOfDate(),
        calc.getInvoiceValue(), calc.getCustomsValue(), calc.getTotalLandedCost(), calc.getUnitLandedCost(),
        calc.getQuantity(), respComps, missing,
        req.originCountry(), req.exportCountry(), req.destinationCountry(), req.hsCode());
  }

  private record FxSnap(BigDecimal amount, String note) {}

  private FxSnap convertAmount(BigDecimal amount, String from, String to, LocalDate asOf, List<String> missing) {
    if (amount == null) return new FxSnap(null, "null amount");
    if (from == null || to == null || from.equalsIgnoreCase(to)) {
      return new FxSnap(amount, "IDENTITY " + (from != null ? from : to));
    }
    if (asOf == null) {
      missing.add("fxAsOfDate");
      return new FxSnap(null, "FX requires asOfDate");
    }
    try {
      ConvertResponse r = currencyService.convert(new ConvertRequest(amount, from, to, asOf));
      return new FxSnap(r.convertedAmount(),
          "FX " + from + "->" + to + " rate=" + r.rateUsed() + " src=" + r.rateSource() + " asOf=" + asOf);
    } catch (Exception ex) {
      missing.add("fxRate");
      return new FxSnap(null, "FX failed: " + ex.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public CalculationResponse get(String id) {
    String customerId = tenantAccessGuard.requireCustomerId();
    ImportCostCalculation calc = calculationRepository.findByCustomerIdAndCalculationId(customerId, id)
        .orElseThrow(() -> new BusinessRuleViolationException("Calculation not found", "IMPORT_NOT_FOUND"));
    List<ImportCostComponent> comps = componentRepository.findByCustomerIdAndCalculationIdOrderBySortOrderAsc(customerId, id);
    List<ComponentResponse> resp = comps.stream()
        .map(c -> new ComponentResponse(c.getComponentCode(), c.getAmount(), c.getCurrencyCode(),
            c.isRecoverable(), c.isIncludedInLanded(), c.getAllocationMethod(), c.getAllocationPct(), c.getFormulaNote()))
        .toList();
    return new CalculationResponse(calc.getCalculationId(), calc.getLifecycleStatus(), calc.getReviewStatus(),
        calc.getCurrencyCode(), calc.getCostingDate(), calc.getFxAsOfDate(),
        calc.getInvoiceValue(), calc.getCustomsValue(), calc.getTotalLandedCost(), calc.getUnitLandedCost(),
        calc.getQuantity(), resp, parseMissing(calc.getMissingDataJson()), null, null, null, null);
  }

  @Transactional(readOnly = true)
  public List<ComponentResponse> components(String id) { return get(id).components(); }

  @Transactional
  public CalculationResponse transition(String id, LifecycleRequest req) {
    String customerId = tenantAccessGuard.requireCustomerId();
    ImportCostCalculation calc = calculationRepository.findByCustomerIdAndCalculationId(customerId, id)
        .orElseThrow(() -> new BusinessRuleViolationException("Calculation not found", "IMPORT_NOT_FOUND"));
    ImportLifecycleStatus current = ImportLifecycleStatus.valueOf(calc.getLifecycleStatus());
    String action = req.action() == null ? "" : req.action().trim().toUpperCase();
    ImportLifecycleStatus next = switch (action) {
      case "SUBMIT" -> {
        if (current != ImportLifecycleStatus.DRAFT)
          throw new BusinessRuleViolationException("Only DRAFT can be submitted", "IMPORT_INVALID_TRANSITION");
        yield ImportLifecycleStatus.SUBMITTED;
      }
      case "APPROVE" -> {
        if (current != ImportLifecycleStatus.DRAFT && current != ImportLifecycleStatus.SUBMITTED)
          throw new BusinessRuleViolationException("Only DRAFT/SUBMITTED can be approved", "IMPORT_INVALID_TRANSITION");
        calc.setApprovedBy(req.actorUserId());
        calc.setApprovedAt(Instant.now());
        yield ImportLifecycleStatus.APPROVED;
      }
      case "LOCK" -> {
        if (current != ImportLifecycleStatus.APPROVED)
          throw new BusinessRuleViolationException("Only APPROVED can be locked", "IMPORT_INVALID_TRANSITION");
        calc.setLockedAt(Instant.now());
        yield ImportLifecycleStatus.LOCKED;
      }
      default -> throw new BusinessRuleViolationException("Unknown action: " + action, "IMPORT_INVALID_ACTION");
    };
    calc.setLifecycleStatus(next.name());
    calc.setUpdatedAt(Instant.now());
    calculationRepository.save(calc);
    return get(id);
  }

  @Transactional
  public void delete(String id) {
    String customerId = tenantAccessGuard.requireCustomerId();
    ImportCostCalculation calc = calculationRepository.findByCustomerIdAndCalculationId(customerId, id)
        .orElseThrow(() -> new BusinessRuleViolationException("Calculation not found", "IMPORT_NOT_FOUND"));
    if (!ImportLifecycleStatus.valueOf(calc.getLifecycleStatus()).isMutable()) {
      throw new BusinessRuleViolationException("Cannot delete non-mutable calculation", "IMPORT_FROZEN");
    }
    componentRepository.deleteByCalculationId(id);
    calculationRepository.delete(calc);
  }

  private void validateInputs(CalculateRequest req, List<String> missing) {
    if (req == null) throw new BusinessRuleViolationException("Request required", "IMPORT_REQ");
    boolean multi = req.lines() != null && !req.lines().isEmpty();
    if (!multi) {
      if (req.originCountry() == null || req.originCountry().isBlank()) missing.add("originCountry");
      if (req.exportCountry() == null || req.exportCountry().isBlank()) missing.add("exportCountry");
      if (req.destinationCountry() == null || req.destinationCountry().isBlank()) missing.add("destinationCountry");
      if (req.hsCode() == null || req.hsCode().isBlank()) missing.add("hsCode");
      if (req.quantity() == null) missing.add("quantity");
      else if (req.quantity().compareTo(BigDecimal.ZERO) <= 0)
        throw new BusinessRuleViolationException("Quantity must be positive", "IMPORT_QTY_INVALID");
    } else {
      if (req.originCountry() == null) missing.add("originCountry");
      if (req.destinationCountry() == null) missing.add("destinationCountry");
    }
  }

  private static void addCharge(List<LandedCostEngine.ComponentLine> comps, String code, BigDecimal amt) {
    if (amt != null) comps.add(new LandedCostEngine.ComponentLine(code, amt, false, true, code));
  }

  private static String toJsonArray(List<String> items) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append('"').append(items.get(i).replace("\"", "\\\"")).append('"');
    }
    return sb.append(']').toString();
  }

  private static List<String> parseMissing(String json) {
    if (json == null || json.isBlank()) return List.of();
    String inner = json.trim();
    if (inner.startsWith("[")) inner = inner.substring(1);
    if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
    if (inner.isBlank()) return List.of();
    List<String> out = new ArrayList<>();
    for (String p : inner.split(",")) {
      String s = p.trim().replace("\"", "");
      if (!s.isEmpty()) out.add(s);
    }
    return out;
  }
}
