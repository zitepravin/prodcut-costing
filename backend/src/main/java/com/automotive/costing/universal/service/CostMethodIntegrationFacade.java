package com.automotive.costing.universal.service;

import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.material.engine.MaterialCostEngine;
import com.automotive.costing.process.engine.ProcessCostEngine;
import com.automotive.costing.shouldcost.engine.ShouldCostEngine;
import com.automotive.costing.universal.domain.CostMethodType;
import com.automotive.costing.universal.dto.UniversalCostingDtos.*;
import com.automotive.costing.universal.engine.AbcAllocationEngine;
import com.automotive.costing.universal.engine.UniversalCostCalculationEngine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Routes CostMethodType to existing pure engines. Does not copy formulas. */
@Component
public class CostMethodIntegrationFacade {

    public static final int MONEY_SCALE = 6;
    public static final RoundingMode RM = RoundingMode.HALF_UP;

    public record IntegrationResult(List<CostComponentResult> components, BigDecimal technicalTotal, BigDecimal allocatedIndirect) {}

    private final AbcAllocationEngine abcAllocationEngine;
    private final UniversalCostCalculationEngine universalEngine;

    public CostMethodIntegrationFacade(AbcAllocationEngine abcAllocationEngine, UniversalCostCalculationEngine universalEngine) {
        this.abcAllocationEngine = abcAllocationEngine;
        this.universalEngine = universalEngine;
    }

    public IntegrationResult integrate(CostMethodType method, CalculateRequest request, String currency) {
        return switch (method) {
            case SHOULD -> integrateShould(request, currency);
            case PROCESS -> integrateProcess(request, currency);
            case ABC -> integrateAbc(request, currency);
            case HYBRID -> integrateHybrid(request, currency);
            case STANDARD, ESTIMATED, ACTUAL, TARGET, QUOTATION, JOB, PROJECT, LIFECYCLE, AVERAGE ->
                    integrateFromComponents(method, request, currency);
        };
    }

    private IntegrationResult integrateShould(CalculateRequest request, String currency) {
        if (request.shouldInput() != null) {
            ShouldEngineInput in = request.shouldInput();
            var result = ShouldCostEngine.calculateFullBuildUp(new ShouldCostEngine.FullBuildUpInput(
                    nz(in.materialCost()), nz(in.processCost()), nz(in.toolingCost()),
                    nz(in.packagingFreightCost()), nz(in.overheadCost()), nz(in.sgaPct()), nz(in.marginPct())));
            List<CostComponentResult> comps = new ArrayList<>();
            comps.add(comp("MATERIAL", nz(in.materialCost()), currency, "ShouldCostEngine.calculateFullBuildUp"));
            comps.add(comp("PROCESS", nz(in.processCost()), currency, "ShouldCostEngine.calculateFullBuildUp"));
            comps.add(comp("TOOLING", nz(in.toolingCost()), currency, "ShouldCostEngine.calculateFullBuildUp"));
            comps.add(comp("PACKAGING_FREIGHT", nz(in.packagingFreightCost()), currency, "ShouldCostEngine.calculateFullBuildUp"));
            comps.add(comp("OVERHEAD", nz(in.overheadCost()), currency, "ShouldCostEngine.calculateFullBuildUp"));
            return new IntegrationResult(comps, result.subtotal(), BigDecimal.ZERO.setScale(MONEY_SCALE, RM));
        }
        return integrateFromComponents(CostMethodType.SHOULD, request, currency);
    }

    private IntegrationResult integrateProcess(CalculateRequest request, String currency) {
        List<CostComponentResult> comps = new ArrayList<>();
        if (request.processInput() != null) {
            ProcessEngineInput in = request.processInput();
            BigDecimal batchSize = in.lotSize() != null && in.lotSize().compareTo(BigDecimal.ZERO) > 0 ? in.lotSize() : BigDecimal.ONE;
            BigDecimal setupSeconds = in.setupTimeMinutes() != null ? in.setupTimeMinutes().multiply(new BigDecimal("60")) : BigDecimal.ZERO;
            var opResult = ProcessCostEngine.calculateOperation(new ProcessCostEngine.OperationCostInput(
                    in.operationId() != null ? in.operationId() : "OP-1",
                    nz(in.cycleTimeSeconds()), nz(in.machineRate()), nz(in.laborRate()),
                    BigDecimal.ONE, setupSeconds, batchSize));
            comps.add(comp("PROCESS_RUN", opResult.runCost(), currency, "ProcessCostEngine.calculateOperation"));
            if (opResult.setupCostPerPiece() != null) {
                comps.add(comp("SETUP", opResult.setupCostPerPiece(), currency, "ProcessCostEngine.calculateOperation"));
            }
            BigDecimal total = opResult.totalOperationCost() != null ? opResult.totalOperationCost() : sum(comps);
            return new IntegrationResult(comps, total.setScale(MONEY_SCALE, RM), BigDecimal.ZERO.setScale(MONEY_SCALE, RM));
        }
        if (request.materialInput() != null) comps.addAll(materialComponents(request.materialInput(), currency));
        comps.addAll(fromDirect(request.directComponents(), currency));
        return finalizeWithUniversal(CostMethodType.PROCESS, comps, request, null);
    }

    /** ABC allocation applied once by UniversalCostCalculationEngine — do not also embed ABC lines in comps. */
    private IntegrationResult integrateAbc(CalculateRequest request, String currency) {
        List<CostComponentResult> comps = new ArrayList<>();
        if (request.materialInput() != null) comps.addAll(materialComponents(request.materialInput(), currency));
        comps.addAll(fromDirect(request.directComponents(), currency));
        return finalizeWithUniversal(CostMethodType.ABC, comps, request, buildAbc(request));
    }

    private IntegrationResult integrateHybrid(CalculateRequest request, String currency) {
        List<CostComponentResult> comps = new ArrayList<>();
        if (request.materialInput() != null) comps.addAll(materialComponents(request.materialInput(), currency));
        if (request.processInput() != null) {
            var pr = integrateProcess(new CalculateRequest(
                    request.costObjectId(), "PROCESS", request.calculationDate(), request.asOfDate(),
                    request.quantity(), null, null, null, null, null, request.processInput(), null, null, null), currency);
            comps.addAll(pr.components());
        }
        comps.addAll(fromDirect(request.directComponents(), currency));
        return finalizeWithUniversal(CostMethodType.HYBRID, comps, request, buildAbc(request));
    }

    private IntegrationResult integrateFromComponents(CostMethodType method, CalculateRequest request, String currency) {
        List<CostComponentResult> comps = new ArrayList<>();
        if (request.materialInput() != null) comps.addAll(materialComponents(request.materialInput(), currency));
        comps.addAll(fromDirect(request.directComponents(), currency));
        if (comps.isEmpty()) {
            throw new BusinessRuleViolationException("No cost components provided for method " + method, "UC-INT-01");
        }
        return finalizeWithUniversal(method, comps, request, null);
    }

    private AbcAllocationEngine.AllocationResult buildAbc(CalculateRequest request) {
        if (request.abcUsages() == null || request.abcUsages().isEmpty()) return null;
        List<AbcAllocationEngine.PoolInput> pools = new ArrayList<>();
        List<AbcAllocationEngine.UsageInput> usages = new ArrayList<>();
        for (AbcPoolUsageInput u : request.abcUsages()) {
            if (u == null || u.poolCode() == null) continue;
            pools.add(new AbcAllocationEngine.PoolInput(u.poolCode(), u.poolAmount(), u.totalDriverQuantity()));
            usages.add(new AbcAllocationEngine.UsageInput(u.poolCode(), u.objectDriverUsage()));
        }
        if (pools.isEmpty()) return null;
        return abcAllocationEngine.allocate(pools, usages);
    }

    private List<CostComponentResult> materialComponents(MaterialEngineInput in, String currency) {
        var result = MaterialCostEngine.calculate(new MaterialCostEngine.MaterialCostInput(
                nz(in.netWeight()),
                in.scrapPct() != null ? in.scrapPct() : BigDecimal.ZERO,
                nz(in.rate()), null, null, null));
        List<CostComponentResult> list = new ArrayList<>();
        list.add(CostComponentResult.builder("MATERIAL")
                .amount(result.materialCost().setScale(MONEY_SCALE, RM))
                .quantity(result.grossWeight()).uom("KG").rate(in.rate()).currency(currency)
                .formulaCode("MaterialCostEngine.calculate")
                .explanation("Gross weight from net/scrap; amount = gross x rate").build());
        return list;
    }

    private IntegrationResult finalizeWithUniversal(CostMethodType method, List<CostComponentResult> comps,
                                                    CalculateRequest request, AbcAllocationEngine.AllocationResult abc) {
        var out = universalEngine.calculate(new UniversalCostCalculationEngine.CalculationInput(
                method, comps, abc, request.quantity(), request.scrapFactorPercent()));
        return new IntegrationResult(out.components(), out.totalCost(), out.allocatedIndirect());
    }

    private List<CostComponentResult> fromDirect(List<ComponentInput> inputs, String currency) {
        List<CostComponentResult> list = new ArrayList<>();
        if (inputs == null) return list;
        for (ComponentInput c : inputs) {
            if (c == null || c.componentCode() == null) continue;
            list.add(CostComponentResult.builder(c.componentCode())
                    .amount(c.amount() != null ? c.amount() : BigDecimal.ZERO)
                    .quantity(c.quantity()).uom(c.uom()).rate(c.rate())
                    .currency(c.currency() != null ? c.currency() : currency).build());
        }
        return list;
    }

    private CostComponentResult comp(String code, BigDecimal amount, String currency, String formula) {
        return CostComponentResult.builder(code)
                .amount(amount != null ? amount.setScale(MONEY_SCALE, RM) : BigDecimal.ZERO.setScale(MONEY_SCALE, RM))
                .currency(currency).formulaCode(formula).build();
    }
    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private BigDecimal sum(List<CostComponentResult> comps) {
        BigDecimal t = BigDecimal.ZERO;
        for (CostComponentResult c : comps) if (c.amount() != null) t = t.add(c.amount());
        return t;
    }
}
