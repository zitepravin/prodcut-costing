package com.automotive.costing.universal.dto;
import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate; import java.util.List;
public final class UniversalCostingDtos {
  private UniversalCostingDtos() {}
  public record CostObjectRequest(String objectType, String objectCode, String objectName, String currencyCode, String uom, String referenceId) {}
  public record CostObjectResponse(String costObjectId, String customerId, String objectType, String objectCode, String objectName, String currencyCode, String uom, String referenceId, String status, Instant createdAt, Instant updatedAt) {}
  public record CostDriverRequest(String driverCode, String driverName, String driverType, String uom, String description) {}
  public record CostDriverResponse(String costDriverId, String customerId, String driverCode, String driverName, String driverType, String uom, String description, boolean active, int versionNo, String lifecycleStatus, Instant createdAt) {}
  public record CostPoolRequest(String poolCode, String poolName, BigDecimal totalAmount, String currencyCode, String costDriverId, String periodCode) {}
  public record CostPoolResponse(String costPoolId, String customerId, String poolCode, String poolName, BigDecimal totalAmount, String currencyCode, String costDriverId, String periodCode, boolean active, int versionNo, String lifecycleStatus, Instant createdAt) {}
  public record ComponentInput(String componentCode, BigDecimal amount, BigDecimal quantity, String uom, BigDecimal rate, String currency) {}
  public record AbcPoolUsageInput(String poolCode, BigDecimal poolAmount, BigDecimal totalDriverQuantity, BigDecimal objectDriverUsage) {}
  public record MaterialEngineInput(BigDecimal netWeight, BigDecimal scrapPct, BigDecimal rate, String currency) {}
  public record ProcessEngineInput(String operationId, BigDecimal cycleTimeSeconds, BigDecimal machineRate, BigDecimal laborRate, BigDecimal setupTimeMinutes, BigDecimal lotSize) {}
  public record ShouldEngineInput(BigDecimal materialCost, BigDecimal processCost, BigDecimal toolingCost, BigDecimal packagingFreightCost, BigDecimal overheadCost, BigDecimal sgaPct, BigDecimal marginPct) {}
  public record CommercialInput(BigDecimal discountAmount, BigDecimal negotiationAdjustment, BigDecimal royaltyAmount, BigDecimal commercialExpenseAmount, BigDecimal profitMarginAmount) {}
  public record CalculateRequest(String costObjectId, String costMethod, LocalDate calculationDate, LocalDate asOfDate, BigDecimal quantity, BigDecimal scrapFactorPercent, String industryProfile, List<ComponentInput> directComponents, List<AbcPoolUsageInput> abcUsages, MaterialEngineInput materialInput, ProcessEngineInput processInput, ShouldEngineInput shouldInput, CommercialInput commercial, String allocationGroupCode) {}
  public record CalculateResponse(String calculationId, String costObjectId, String costMethod, LocalDate calculationDate, String currencyCode, BigDecimal technicalTotal, BigDecimal commercialTotal, BigDecimal totalCost, BigDecimal unitCost, BigDecimal sellingPrice, BigDecimal quantity, BigDecimal allocatedIndirect, String lifecycleStatus, String industryProfile, Instant createdAt) {}
  public record LifecycleTransitionRequest(String action, String actorUserId) {}
  public record NormRequest(String normCode, String description, String unit, String parameterKey, String formulaCode, BigDecimal normValue, String objectScope, String objectReferenceId, LocalDate effectiveFrom, LocalDate effectiveTo, String source) {}
  public record NormResponse(String normId, String customerId, String normCode, String description, String unit, String parameterKey, String formulaCode, BigDecimal normValue, String objectScope, String objectReferenceId, LocalDate effectiveFrom, LocalDate effectiveTo, int versionNo, boolean active, String approvalStatus, String source, Instant createdAt) {}
  public record SobRequest(String costObjectId, String productCode, String supplierId, BigDecimal allocationPercent, String allocationBasis, String allocationGroupCode, LocalDate effectiveFrom, LocalDate effectiveTo, Integer priority) {}
  public record SobResponse(String sobId, String customerId, String costObjectId, String productCode, String supplierId, BigDecimal allocationPercent, String allocationBasis, String allocationGroupCode, LocalDate effectiveFrom, LocalDate effectiveTo, String status, int priority, int versionNo, Instant createdAt) {}
  public record IndustryProfileResponse(String profileId, String profileCode, String profileName, String description, String defaultMethodsJson, String defaultComponentsJson, String defaultDriversJson, boolean active) {}
}
