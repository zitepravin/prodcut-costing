package com.automotive.costing.process.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.bom.service.BomNodeService;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.entity.LaborMaster;
import com.automotive.costing.masterdata.entity.MachineMaster;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.repository.LaborMasterRepository;
import com.automotive.costing.masterdata.repository.MachineMasterRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.process.dto.ProcessCostDtos.*;
import com.automotive.costing.process.engine.ConsumableCostEngine.ConsumableLine;
import com.automotive.costing.process.engine.ConsumableCostEngine.ConsumableType;
import com.automotive.costing.process.engine.InspectionCostEngine.InspectionActivity;
import com.automotive.costing.process.engine.ProcessCostEngine;
import com.automotive.costing.process.engine.ProcessCostEngine.OperationComponentResult;
import com.automotive.costing.process.engine.ProcessCostEngine.OperationCostInput;
import com.automotive.costing.process.engine.ProcessCostEngine.OperationCostResult;
import com.automotive.costing.process.engine.ProcessCostEngine.RoutingCostResult;
import com.automotive.costing.process.engine.ProcessCostEngine.SecondaryCostInput;
import com.automotive.costing.process.entity.ProcessCostCalculation;
import com.automotive.costing.process.entity.ProcessOperation;
import com.automotive.costing.process.entity.ProcessRouting;
import com.automotive.costing.process.repository.ProcessCostCalculationRepository;
import com.automotive.costing.process.repository.ProcessOperationRepository;
import com.automotive.costing.process.repository.ProcessRoutingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * M04 Process &amp; Labor Costing - cost calculation (PRC-02, PRC-04, PRC-06, PRC-07)
 * extended for V277 POWER / CONSUMABLE / INSPECTION component integration.
 */
@Service
@RequiredArgsConstructor
public class ProcessCostService {

    private static final String ENTITY_TYPE = "ProcessCostCalculation";
    private static final int SCALE = 6;

    private final ProcessRoutingRepository routingRepository;
    private final ProcessOperationRepository operationRepository;
    private final ProcessCostCalculationRepository calculationRepository;
    private final MachineMasterRepository machineMasterRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final LaborMasterRepository laborMasterRepository;
    private final BomNodeRepository bomNodeRepository;
    private final BomNodeService bomNodeService;
    private final AuditService auditService;

    @Transactional
    public Response calculate(CalculateRequest request) {
        ProcessRouting routing = routingRepository.findByCustomerIdAndRoutingId(
                        tenantAccessGuard.requireCustomerId(), request.routingId())
                .orElseThrow(() -> ResourceNotFoundException.of("ProcessRouting", request.routingId()));

        LocalDate costingDate = request.costingDate() != null ? request.costingDate() : LocalDate.now();
        ComponentRoutingResult result = computeRoutingCostWithComponents(routing, costingDate);

        ProcessCostCalculation entity = new ProcessCostCalculation();
        entity.setCalculationId(UUID.randomUUID().toString());
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setRoutingId(routing.getRoutingId());
        entity.setBomNodeId(routing.getBomNodeId());
        entity.setTotalProcessCost(result.totalProcessCost());
        entity.setSetupCost(result.setupCost());
        entity.setMachineCost(result.machineCost());
        entity.setDirectLaborCost(result.directLaborCost());
        entity.setPowerCost(result.powerCost());
        entity.setConsumableCost(result.consumableCost());
        entity.setInspectionCost(result.inspectionCost());
        entity.setExternalProcessCost(result.externalProcessCost());
        entity.setComponentBreakdown(result.componentSummary());
        entity.setCostingDate(costingDate);
        entity.setCalculationBasis(result.calculationBasis());
        entity.setCalculatedAt(Instant.now());

        ProcessCostCalculation saved = calculationRepository.save(entity);
        auditService.recordCreate(ENTITY_TYPE, saved.getCalculationId(), toResponse(saved, result));

        BomNode bomNode = bomNodeRepository.findByBomNodeId(routing.getBomNodeId())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", routing.getBomNodeId()));
        // Propagate explicit costing date into BOM roll-up FX path
        bomNodeService.triggerRollupForProgram(bomNode.getProgramId(), costingDate);

        return toResponse(saved, result);
    }

    @Transactional(readOnly = true)
    public Response getLatest(String routingId) {
        ProcessCostCalculation c = calculationRepository
                .findFirstByCustomerIdAndRoutingIdOrderByCalculatedAtDesc(
                        tenantAccessGuard.requireCustomerId(), routingId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcessCostCalculation", routingId));
        return toResponseFromEntity(c);
    }

    @Transactional(readOnly = true)
    public CompareResponse compareRoutings(CompareRequest request) {
        if (request.routingIds() == null || request.routingIds().size() < 2) {
            throw new BusinessRuleViolationException(
                    "At least two routing IDs are required to run a comparison.", "PRC-07");
        }
        LocalDate costingDate = request.costingDate() != null ? request.costingDate() : LocalDate.now();
        List<CompareLine> lines = request.routingIds().stream()
                .map(id -> {
                    ProcessRouting routing = routingRepository.findByCustomerIdAndRoutingId(
                                    tenantAccessGuard.requireCustomerId(), id)
                            .orElseThrow(() -> ResourceNotFoundException.of("ProcessRouting", id));
                    ComponentRoutingResult result = computeRoutingCostWithComponents(routing, costingDate);
                    return new CompareLine(id, result.totalProcessCost(), result.operationCount());
                })
                .toList();
        String lowest = lines.stream()
                .min((a, b) -> a.totalProcessCost().compareTo(b.totalProcessCost()))
                .map(CompareLine::routingId)
                .orElse(null);
        return new CompareResponse(lines, lowest);
    }

    /**
     * Backward-compatible pure total path used by compare when only totals needed.
     */
    private RoutingCostResult computeRoutingCost(ProcessRouting routing) {
        List<ProcessOperation> operations =
                operationRepository.findByRoutingIdOrderBySequenceNumberAsc(routing.getRoutingId());
        if (operations.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Routing " + routing.getRoutingId() + " has no operations to calculate.", "PRC-06");
        }
        List<OperationCostInput> inputs = operations.stream()
                .map(this::toOperationCostInput)
                .toList();
        return ProcessCostEngine.calculateRouting(inputs);
    }

    private ComponentRoutingResult computeRoutingCostWithComponents(ProcessRouting routing, LocalDate costingDate) {
        List<ProcessOperation> operations =
                operationRepository.findByRoutingIdOrderBySequenceNumberAsc(routing.getRoutingId());
        if (operations.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Routing " + routing.getRoutingId() + " has no operations to calculate.", "PRC-06");
        }

        BigDecimal setup = BigDecimal.ZERO;
        BigDecimal machine = BigDecimal.ZERO;
        BigDecimal labor = BigDecimal.ZERO;
        BigDecimal power = BigDecimal.ZERO;
        BigDecimal consumable = BigDecimal.ZERO;
        BigDecimal inspection = BigDecimal.ZERO;
        BigDecimal external = BigDecimal.ZERO;
        List<OperationCostLine> opLines = new ArrayList<>();
        List<String> basisParts = new ArrayList<>();
        List<ComponentLine> allComponents = new ArrayList<>();

        for (ProcessOperation op : operations) {
            OperationCostInput input = toOperationCostInput(op);
            SecondaryCostInput secondary = buildSecondary(op, routing.getBomNodeId(), costingDate);
            OperationComponentResult r = ProcessCostEngine.calculateOperationWithComponents(input, secondary);

            setup = setup.add(r.setupCost());
            machine = machine.add(r.machineCost());
            labor = labor.add(r.directLaborCost());
            power = power.add(r.powerCost());
            consumable = consumable.add(r.consumableCost());
            inspection = inspection.add(r.inspectionCost());
            external = external.add(r.externalProcessCost());

            opLines.add(new OperationCostLine(
                    r.operationId(),
                    r.machineCost().add(r.directLaborCost()),
                    r.setupCost(),
                    r.totalOperationCost(),
                    r.setupCost(), r.machineCost(), r.directLaborCost(),
                    r.powerCost(), r.consumableCost(), r.inspectionCost(), r.externalProcessCost(),
                    r.components().stream()
                            .map(c -> new ComponentLine(c.componentCode(), c.amount(), c.currency(),
                                    c.quantity(), c.uom(), c.rate(), c.rateSource(), c.formulaCode(),
                                    c.explanation()))
                            .toList()
            ));
            basisParts.add(r.calculationBasis());
            for (CostComponentResult c : r.components()) {
                allComponents.add(new ComponentLine(c.componentCode(), c.amount(), c.currency(),
                        c.quantity(), c.uom(), c.rate(), c.rateSource(), c.formulaCode(), c.explanation()));
            }
        }

        BigDecimal total = setup.add(machine).add(labor).add(power).add(consumable).add(inspection).add(external)
                .setScale(SCALE, RoundingMode.HALF_UP);

        String summary = allComponents.stream()
                .map(c -> c.componentCode() + "=" + c.amount().toPlainString())
                .collect(Collectors.joining("; "));
        if (summary.length() > 4000) {
            summary = summary.substring(0, 4000);
        }

        return new ComponentRoutingResult(
                total,
                setup.setScale(SCALE, RoundingMode.HALF_UP),
                machine.setScale(SCALE, RoundingMode.HALF_UP),
                labor.setScale(SCALE, RoundingMode.HALF_UP),
                power.setScale(SCALE, RoundingMode.HALF_UP),
                consumable.setScale(SCALE, RoundingMode.HALF_UP),
                inspection.setScale(SCALE, RoundingMode.HALF_UP),
                external.setScale(SCALE, RoundingMode.HALF_UP),
                opLines,
                operations.size(),
                String.join(" | ", basisParts),
                summary,
                allComponents
        );
    }


    private List<ConsumableLine> parseConsumables(ProcessOperation op) {
        String json = op.getConsumablesJson();
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            List<ConsumableLine> lines = new ArrayList<>();
            String body = json.trim();
            if (!body.startsWith("[")) {
                return null;
            }
            for (String part : body.replace("[", "").replace("]", "").split("},")) {
                String obj = part.replace("{", "").replace("}", "");
                if (obj.isBlank()) {
                    continue;
                }
                String type = extractJsonField(obj, "type");
                String qty = extractJsonField(obj, "quantity");
                String uom = extractJsonField(obj, "uom");
                String rate = extractJsonField(obj, "unitRate");
                String ccy = extractJsonField(obj, "rateCurrency");
                String src = extractJsonField(obj, "rateSource");
                String eff = extractJsonField(obj, "rateEffectiveDate");
                if (type == null || qty == null || rate == null) {
                    continue;
                }
                ConsumableType ct = ConsumableType.valueOf(type.trim().toUpperCase());
                LocalDate effDate = (eff == null || eff.isBlank()) ? null : LocalDate.parse(eff.trim());
                lines.add(new ConsumableLine(ct, new BigDecimal(qty.trim()), uom,
                        new BigDecimal(rate.trim()), ccy, src, effDate));
            }
            return lines.isEmpty() ? null : lines;
        } catch (Exception e) {
            throw new BusinessRuleViolationException(
                    "Invalid consumables_json on operation " + op.getOperationId() + ": " + e.getMessage(),
                    "CON-JSON");
        }
    }

    private static String extractJsonField(String obj, String key) {
        String quotedKey = "\"" + key + "\"";
        int i = obj.indexOf(quotedKey);
        if (i < 0) {
            return null;
        }
        int colon = obj.indexOf(':', i);
        if (colon < 0) {
            return null;
        }
        String rest = obj.substring(colon + 1).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf('"', 1);
            return end > 0 ? rest.substring(1, end) : null;
        }
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < rest.length(); c++) {
            char ch = rest.charAt(c);
            if (ch == ',' || ch == '}') {
                break;
            }
            if (!Character.isWhitespace(ch)) {
                sb.append(ch);
            }
        }
        String v = sb.toString();
        return "null".equals(v) || v.isEmpty() ? null : v;
    }

    private SecondaryCostInput buildSecondary(ProcessOperation op, String bomNodeId, LocalDate costingDate) {
        BigDecimal machineKw = op.getMachineKw();
        BigDecimal electricityRate = op.getElectricityRate();
        String rateCcy = op.getElectricityRateCurrency();
        String rateSource = null;
        LocalDate rateEff = null;

        // Fallback: use MachineMaster.powerCostPerHour as electricity equivalent via kW=1 synthetic
        // when operation lacks kW/rate but machine has powerCostPerHour
        if ((machineKw == null || electricityRate == null) && op.getMachineCode() != null) {
            MachineMaster m = machineMasterRepository
                    .findFirstByCustomerIdAndCodeAndStatusOrderByVersionNumberDesc(
                            tenantAccessGuard.requireCustomerId(), op.getMachineCode(), MasterDataStatus.ACTIVE)
                    .orElse(null);
            if (m != null && m.getPowerCostPerHour() != null
                    && m.getPowerCostPerHour().compareTo(BigDecimal.ZERO) > 0) {
                // Represent powerCostPerHour as 1 kW * rate, so Power = 1 * runtimeHours * powerCostPerHour
                if (machineKw == null) {
                    machineKw = BigDecimal.ONE;
                }
                if (electricityRate == null) {
                    electricityRate = m.getPowerCostPerHour();
                    rateSource = "MACHINE_MASTER.powerCostPerHour";
                    rateEff = costingDate;
                }
            }
        }

        List<InspectionActivity> inspections = null;
        if (op.getInspectionTimeHours() != null || op.getInspectorRate() != null) {
            // Only build inspection when rate is present; missing rate surfaces via engine
            inspections = List.of(new InspectionActivity(
                    op.getOperationId() + "-insp",
                    op.getInspectionTimeHours() != null ? op.getInspectionTimeHours() : BigDecimal.ZERO,
                    op.getInspectorRate(),
                    "USD",
                    "OPERATION",
                    costingDate,
                    op.getInspectionFixedCharge()
            ));
        }

        // Only attach secondary when at least one of power/inspection configured
        List<ConsumableLine> consumables = parseConsumables(op);
        if (machineKw == null && electricityRate == null && inspections == null && consumables == null) {
            return null;
        }

        return new SecondaryCostInput(
                machineKw,
                electricityRate,
                rateCcy != null ? rateCcy : "USD",
                rateSource,
                rateEff,
                consumables,
                inspections,
                null,
                costingDate,
                bomNodeId
        );
    }

    private OperationCostInput toOperationCostInput(ProcessOperation op) {
        BigDecimal machineRate = resolveMachineRate(op.getMachineCode());
        BigDecimal laborRate = resolveLaborRate(op.getLaborCode());
        return new OperationCostInput(op.getOperationId(), op.getCycleTimeSeconds(), machineRate, laborRate,
                op.getOperatorCount(), op.getSetupTimeSeconds(), op.getBatchSize(), op.getOutputPartsPerCycle());
    }

    private BigDecimal resolveMachineRate(String machineCode) {
        if (machineCode == null || machineCode.isBlank()) {
            return null;
        }
        MachineMaster machine = machineMasterRepository
                .findFirstByCustomerIdAndCodeAndStatusOrderByVersionNumberDesc(
                        tenantAccessGuard.requireCustomerId(), machineCode, MasterDataStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Machine '" + machineCode + "' has no Active rate version - it cannot be used in "
                                + "process costing (SRS 4.5.5).",
                        "SRS-4.5.5-active-rate-required"));
        return machine.getRate();
    }

    private BigDecimal resolveLaborRate(String laborCode) {
        if (laborCode == null || laborCode.isBlank()) {
            return null;
        }
        LaborMaster labor = laborMasterRepository
                .findFirstByCustomerIdAndCodeAndStatusOrderByVersionNumberDesc(
                        tenantAccessGuard.requireCustomerId(), laborCode, MasterDataStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Labor rate '" + laborCode + "' has no Active version - it cannot be used in "
                                + "process costing.",
                        "SRS-4.5.5-active-rate-required"));
        return labor.getRate();
    }

    private Response toResponse(ProcessCostCalculation c, ComponentRoutingResult result) {
        return new Response(
                c.getCalculationId(), c.getRoutingId(), c.getBomNodeId(), c.getTotalProcessCost(),
                result.operationLines(),
                c.getSetupCost(), c.getMachineCost(), c.getDirectLaborCost(),
                c.getPowerCost(), c.getConsumableCost(), c.getInspectionCost(), c.getExternalProcessCost(),
                result.allComponents(),
                c.getCostingDate(),
                c.getCalculationBasis(), c.getCalculatedAt());
    }

    private Response toResponseFromEntity(ProcessCostCalculation c) {
        return new Response(
                c.getCalculationId(), c.getRoutingId(), c.getBomNodeId(), c.getTotalProcessCost(),
                List.of(),
                c.getSetupCost(), c.getMachineCost(), c.getDirectLaborCost(),
                c.getPowerCost(), c.getConsumableCost(), c.getInspectionCost(), c.getExternalProcessCost(),
                List.of(),
                c.getCostingDate(),
                c.getCalculationBasis(), c.getCalculatedAt());
    }

    /** Internal aggregate for multi-operation component totals. */
    private record ComponentRoutingResult(
            BigDecimal totalProcessCost,
            BigDecimal setupCost,
            BigDecimal machineCost,
            BigDecimal directLaborCost,
            BigDecimal powerCost,
            BigDecimal consumableCost,
            BigDecimal inspectionCost,
            BigDecimal externalProcessCost,
            List<OperationCostLine> operationLines,
            int operationCount,
            String calculationBasis,
            String componentSummary,
            List<ComponentLine> allComponents
    ) {}
}
