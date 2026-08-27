package com.automotive.costing.importcosting.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public final class ImportCostingDtos {
  private ImportCostingDtos() {}

  public record LineInput(
      String materialId, String productCode, String hsCode,
      BigDecimal quantity, String uom,
      BigDecimal netWeight, BigDecimal grossWeight, BigDecimal volume,
      BigDecimal unitPrice, String invoiceCurrency, BigDecimal invoiceValue
  ) {}

  public record ChargeInput(String componentCode, BigDecimal amount, String currency, String allocationMethod) {}

  public record CalculateRequest(
      String supplierId, String supplierCountry,
      String originCountry, String exportCountry, String destinationCountry,
      String materialId, String productCode, String hsCode,
      BigDecimal quantity, String uom,
      BigDecimal netWeight, BigDecimal grossWeight, BigDecimal volume,
      String invoiceCurrency, BigDecimal unitPurchasePrice, BigDecimal invoiceValue,
      String incoterm, String shipmentMode,
      LocalDate shipmentDate, LocalDate customsDate, LocalDate costingDate, LocalDate fxAsOfDate,
      String targetCurrency,
      BigDecimal internationalFreight, BigDecimal inlandFreight, BigDecimal insurance,
      BigDecimal packing, BigDecimal originCharges, BigDecimal portCharges,
      BigDecimal customsClearance, BigDecimal brokerage, BigDecimal otherImportCost,
      List<LineInput> lines, List<ChargeInput> extraCharges,
      String allocationMethod, String industryProfile
  ) {}

  public record ComponentResponse(
      String componentCode, BigDecimal amount, String currencyCode,
      boolean recoverable, boolean includedInLanded,
      String allocationMethod, BigDecimal allocationPct, String formulaNote
  ) {}

  public record CalculationResponse(
      String calculationId, String lifecycleStatus, String reviewStatus,
      String currencyCode, LocalDate costingDate, LocalDate fxAsOfDate,
      BigDecimal invoiceValue, BigDecimal customsValue,
      BigDecimal totalLandedCost, BigDecimal unitLandedCost, BigDecimal quantity,
      List<ComponentResponse> components, List<String> missingData,
      String originCountry, String exportCountry, String destinationCountry, String hsCode
  ) {}

  public record LifecycleRequest(String action, String actorUserId) {}
}
