package com.automotive.costing.universal.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.universal.domain.CostLifecycleStatus;
import com.automotive.costing.universal.domain.CostMethodType;
import com.automotive.costing.universal.domain.CostObjectType;
import com.automotive.costing.universal.dto.UniversalCostingDtos.*;
import com.automotive.costing.universal.entity.*;
import com.automotive.costing.universal.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UniversalCostingService {

    private static final int MONEY_SCALE = 6;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private final CostObjectRepository costObjectRepository;
    private final CostDriverRepository costDriverRepository;
    private final CostPoolRepository costPoolRepository;
    private final UniversalCostCalculationRepository calculationRepository;
    private final IndustryCostingProfileRepository profileRepository;
    private final CostMethodIntegrationFacade integrationFacade;
    private final NormDefinitionService normDefinitionService;
    private final SupplierSobService supplierSobService;
    private final TenantAccessGuard tenantAccessGuard;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CostObjectResponse> listCostObjects() {
        return costObjectRepository.findByCustomerIdOrderByObjectCodeAsc(tenantAccessGuard.requireCustomerId())
                .stream().map(this::toCostObjectResponse).toList();
    }

    @Transactional(readOnly = true)
    public CostObjectResponse getCostObject(String costObjectId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        CostObject o = costObjectRepository.findByCustomerIdAndCostObjectId(customerId, costObjectId)
                .orElseThrow(() -> new BusinessRuleViolationException("Cost object not found: " + costObjectId, "UC-OBJ-01"));
        return toCostObjectResponse(o);
    }

    @Transactional
    public CostObjectResponse createCostObject(CostObjectRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        if (request.objectType() == null || request.objectType().isBlank())
            throw new BusinessRuleViolationException("objectType is required", "UC-OBJ-03");
        try { CostObjectType.valueOf(request.objectType().trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("Unsupported objectType: " + request.objectType(), "UC-OBJ-04");
        }
        if (request.objectCode() == null || request.objectCode().isBlank())
            throw new BusinessRuleViolationException("objectCode is required", "UC-OBJ-05");
        if (request.objectName() == null || request.objectName().isBlank())
            throw new BusinessRuleViolationException("objectName is required", "UC-OBJ-06");
        if (costObjectRepository.existsByCustomerIdAndObjectCode(customerId, request.objectCode().trim()))
            throw new BusinessRuleViolationException("Cost object code already exists: " + request.objectCode(), "UC-OBJ-02");
        Instant now = Instant.now();
        CostObject o = new CostObject();
        o.setCostObjectId(UUID.randomUUID().toString());
        o.setCustomerId(customerId);
        o.setObjectType(request.objectType().trim().toUpperCase(Locale.ROOT));
        o.setObjectCode(request.objectCode().trim());
        o.setObjectName(request.objectName().trim());
        o.setCurrencyCode(request.currencyCode() != null && !request.currencyCode().isBlank()
                ? request.currencyCode().trim().toUpperCase(Locale.ROOT) : "USD");
        o.setUom(request.uom());
        o.setReferenceId(request.referenceId());
        o.setStatus("ACTIVE");
        o.setCreatedAt(now);
        o.setUpdatedAt(now);
        return toCostObjectResponse(costObjectRepository.save(o));
    }

    @Transactional(readOnly = true)
    public List<CostDriverResponse> listDrivers() {
        return costDriverRepository.findByCustomerIdOrderByDriverCodeAsc(tenantAccessGuard.requireCustomerId())
                .stream().map(this::toDriverResponse).toList();
    }

    @Transactional
    public CostDriverResponse createDriver(CostDriverRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        if (request.driverCode() == null || request.driverCode().isBlank())
            throw new BusinessRuleViolationException("driverCode is required", "UC-DRV-01");
        if (costDriverRepository.existsByCustomerIdAndDriverCode(customerId, request.driverCode().trim()))
            throw new BusinessRuleViolationException("Driver code exists", "UC-DRV-02");
        CostDriver d = new CostDriver();
        d.setCostDriverId(UUID.randomUUID().toString());
        d.setCustomerId(customerId);
        d.setDriverCode(request.driverCode().trim());
        d.setDriverName(request.driverName().trim());
        d.setDriverType(request.driverType().trim().toUpperCase(Locale.ROOT));
        d.setUom(request.uom());
        d.setDescription(request.description());
        d.setActive(true);
        d.setVersionNo(1);
        d.setLifecycleStatus("ACTIVE");
        d.setCreatedAt(Instant.now());
        return toDriverResponse(costDriverRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<CostPoolResponse> listPools() {
        return costPoolRepository.findByCustomerIdOrderByPoolCodeAsc(tenantAccessGuard.requireCustomerId())
                .stream().map(this::toPoolResponse).toList();
    }

    @Transactional
    public CostPoolResponse createPool(CostPoolRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        if (request.poolCode() == null || request.poolCode().isBlank())
            throw new BusinessRuleViolationException("poolCode is required", "UC-POOL-01");
        if (request.totalAmount() != null && request.totalAmount().compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessRuleViolationException("totalAmount cannot be negative", "UC-POOL-02");
        CostPool p = new CostPool();
        p.setCostPoolId(UUID.randomUUID().toString());
        p.setCustomerId(customerId);
        p.setPoolCode(request.poolCode().trim());
        p.setPoolName(request.poolName().trim());
        p.setTotalAmount(request.totalAmount() != null ? request.totalAmount() : BigDecimal.ZERO);
        p.setCurrencyCode(request.currencyCode() != null ? request.currencyCode().trim().toUpperCase(Locale.ROOT) : "USD");
        p.setCostDriverId(request.costDriverId());
        p.setPeriodCode(request.periodCode());
        p.setActive(true);
        p.setVersionNo(1);
        p.setLifecycleStatus("DRAFT");
        p.setCreatedAt(Instant.now());
        return toPoolResponse(costPoolRepository.save(p));
    }

    @Transactional
    public CalculateResponse calculate(CalculateRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        if (request.costObjectId() == null || request.costObjectId().isBlank())
            throw new BusinessRuleViolationException("costObjectId is required", "UC-CALC-01");
        if (request.costMethod() == null || request.costMethod().isBlank())
            throw new BusinessRuleViolationException("costMethod is required", "UC-CALC-02");
        CostObject obj = costObjectRepository.findByCustomerIdAndCostObjectId(customerId, request.costObjectId())
                .orElseThrow(() -> new BusinessRuleViolationException("Cost object not found: " + request.costObjectId(), "UC-CALC-03"));
        CostMethodType method;
        try { method = CostMethodType.valueOf(request.costMethod().trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("Unsupported cost method: " + request.costMethod(), "UC-CALC-04");
        }
        LocalDate asOf = request.asOfDate() != null ? request.asOfDate()
                : (request.calculationDate() != null ? request.calculationDate() : LocalDate.now());
        var integration = integrationFacade.integrate(method, request, obj.getCurrencyCode());
        BigDecimal technical = integration.technicalTotal().setScale(MONEY_SCALE, RM);
        BigDecimal discount = BigDecimal.ZERO, negotiation = BigDecimal.ZERO, royalty = BigDecimal.ZERO;
        BigDecimal commercialExpense = BigDecimal.ZERO, profitMargin = BigDecimal.ZERO;
        if (request.commercial() != null) {
            CommercialInput c = request.commercial();
            discount = nz(c.discountAmount());
            negotiation = nz(c.negotiationAdjustment());
            royalty = nz(c.royaltyAmount());
            commercialExpense = nz(c.commercialExpenseAmount());
            profitMargin = nz(c.profitMarginAmount());
        }
        BigDecimal commercialTotal = royalty.add(commercialExpense).add(profitMargin)
                .add(negotiation).subtract(discount).setScale(MONEY_SCALE, RM);
        BigDecimal totalCost = technical.add(commercialTotal).setScale(MONEY_SCALE, RM);
        BigDecimal unitCost = null;
        if (request.quantity() != null && request.quantity().compareTo(BigDecimal.ZERO) > 0) {
            unitCost = totalCost.divide(request.quantity(), MONEY_SCALE, RM);
        }
        String sobSnapshot = null;
        if (request.allocationGroupCode() != null && !request.allocationGroupCode().isBlank()) {
            sobSnapshot = toJsonSafe(supplierSobService.resolveActive(request.allocationGroupCode(), asOf));
        }
        Instant now = Instant.now();
        UniversalCostCalculation calc = new UniversalCostCalculation();
        calc.setCalculationId(UUID.randomUUID().toString());
        calc.setCustomerId(customerId);
        calc.setCostObjectId(obj.getCostObjectId());
        calc.setCostMethod(method.name());
        calc.setCalculationDate(request.calculationDate() != null ? request.calculationDate() : LocalDate.now());
        calc.setAsOfDate(asOf);
        calc.setCurrencyCode(obj.getCurrencyCode());
        calc.setTotalCost(totalCost);
        calc.setUnitCost(unitCost);
        calc.setQuantity(request.quantity());
        calc.setComponentBreakdown(toJsonSafe(integration.components()));
        calc.setVersionNo(1);
        calc.setLifecycleStatus(CostLifecycleStatus.DRAFT.name());
        calc.setIndustryProfile(request.industryProfile());
        calc.setTechnicalTotal(technical);
        calc.setCommercialTotal(commercialTotal);
        calc.setSellingPrice(totalCost);
        calc.setDiscountAmount(discount);
        calc.setRoyaltyAmount(royalty);
        calc.setProfitMarginAmount(profitMargin);
        calc.setCommercialExpenseAmount(commercialExpense);
        calc.setNegotiationAdjustment(negotiation);
        calc.setSobSnapshotJson(sobSnapshot);
        calc.setCreatedAt(now);
        calculationRepository.save(calc);
        return toCalcResponse(calc, integration.allocatedIndirect());
    }

    @Transactional(readOnly = true)
    public List<CalculateResponse> listCalculations(String costObjectId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        List<UniversalCostCalculation> rows = costObjectId != null && !costObjectId.isBlank()
                ? calculationRepository.findByCustomerIdAndCostObjectIdOrderByCreatedAtDesc(customerId, costObjectId)
                : calculationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return rows.stream().map(c -> toCalcResponse(c, null)).toList();
    }

    @Transactional
    public CalculateResponse transition(String calculationId, LifecycleTransitionRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        UniversalCostCalculation calc = calculationRepository.findByCustomerIdAndCalculationId(customerId, calculationId)
                .orElseThrow(() -> new BusinessRuleViolationException("Calculation not found: " + calculationId, "UC-LC-01"));
        String action = request.action() != null ? request.action().trim().toUpperCase(Locale.ROOT) : "";
        String status = calc.getLifecycleStatus();
        Instant now = Instant.now();
        String actor = request.actorUserId();
        switch (action) {
            case "SUBMIT" -> {
                if (!CostLifecycleStatus.DRAFT.name().equals(status))
                    throw new BusinessRuleViolationException("Only DRAFT can be submitted", "UC-LC-02");
                calc.setLifecycleStatus(CostLifecycleStatus.SUBMITTED.name());
                calc.setSubmittedAt(now); calc.setSubmittedBy(actor);
            }
            case "APPROVE" -> {
                if (!CostLifecycleStatus.SUBMITTED.name().equals(status) && !CostLifecycleStatus.DRAFT.name().equals(status))
                    throw new BusinessRuleViolationException("Only DRAFT/SUBMITTED can be approved", "UC-LC-03");
                calc.setLifecycleStatus(CostLifecycleStatus.APPROVED.name());
                calc.setApprovedAt(now); calc.setApprovedBy(actor);
            }
            case "LOCK" -> {
                if (!CostLifecycleStatus.APPROVED.name().equals(status))
                    throw new BusinessRuleViolationException("Only APPROVED can be locked", "UC-LC-04");
                calc.setLifecycleStatus(CostLifecycleStatus.LOCKED.name());
                calc.setLockedAt(now); calc.setLockedBy(actor);
            }
            case "SUPERSEDE" -> {
                if (!CostLifecycleStatus.LOCKED.name().equals(status) && !CostLifecycleStatus.APPROVED.name().equals(status))
                    throw new BusinessRuleViolationException("Only APPROVED/LOCKED can be superseded", "UC-LC-05");
                calc.setLifecycleStatus(CostLifecycleStatus.SUPERSEDED.name());
            }
            default -> throw new BusinessRuleViolationException("Unknown lifecycle action: " + action, "UC-LC-06");
        }
        calculationRepository.save(calc);
        return toCalcResponse(calc, null);
    }

    public void assertMutable(UniversalCostCalculation calc) {
        String st = calc.getLifecycleStatus();
        if (CostLifecycleStatus.APPROVED.name().equals(st) || CostLifecycleStatus.LOCKED.name().equals(st)) {
            throw new BusinessRuleViolationException("Calculation is " + st + " and immutable", "UC-FREEZE-01");
        }
    }

    @Transactional(readOnly = true)
    public List<IndustryProfileResponse> listIndustryProfiles() {
        return profileRepository.findByActiveTrueOrderByProfileCodeAsc().stream().map(this::toProfileResponse).toList();
    }

    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private String toJsonSafe(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException e) { return null; }
    }
    private CostObjectResponse toCostObjectResponse(CostObject o) {
        return new CostObjectResponse(o.getCostObjectId(), o.getCustomerId(), o.getObjectType(), o.getObjectCode(),
                o.getObjectName(), o.getCurrencyCode(), o.getUom(), o.getReferenceId(), o.getStatus(), o.getCreatedAt(), o.getUpdatedAt());
    }
    private CostDriverResponse toDriverResponse(CostDriver d) {
        return new CostDriverResponse(d.getCostDriverId(), d.getCustomerId(), d.getDriverCode(), d.getDriverName(),
                d.getDriverType(), d.getUom(), d.getDescription(), d.isActive(), d.getVersionNo(), d.getLifecycleStatus(), d.getCreatedAt());
    }
    private CostPoolResponse toPoolResponse(CostPool p) {
        return new CostPoolResponse(p.getCostPoolId(), p.getCustomerId(), p.getPoolCode(), p.getPoolName(),
                p.getTotalAmount(), p.getCurrencyCode(), p.getCostDriverId(), p.getPeriodCode(), p.isActive(),
                p.getVersionNo(), p.getLifecycleStatus(), p.getCreatedAt());
    }
    private CalculateResponse toCalcResponse(UniversalCostCalculation c, BigDecimal allocated) {
        return new CalculateResponse(c.getCalculationId(), c.getCostObjectId(), c.getCostMethod(),
                c.getCalculationDate(), c.getCurrencyCode(), c.getTechnicalTotal(), c.getCommercialTotal(),
                c.getTotalCost(), c.getUnitCost(), c.getSellingPrice(), c.getQuantity(), allocated,
                c.getLifecycleStatus(), c.getIndustryProfile(), c.getCreatedAt());
    }
    private IndustryProfileResponse toProfileResponse(IndustryCostingProfile p) {
        return new IndustryProfileResponse(p.getProfileId(), p.getProfileCode(), p.getProfileName(), p.getDescription(),
                p.getDefaultMethodsJson(), p.getDefaultComponentsJson(), p.getDefaultDriversJson(), p.isActive());
    }
}
