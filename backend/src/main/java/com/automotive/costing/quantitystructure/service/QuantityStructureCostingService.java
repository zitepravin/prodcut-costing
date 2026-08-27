package com.automotive.costing.quantitystructure.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.costingvariant.entity.CostingVariant;
import com.automotive.costing.costingvariant.entity.QuantityStructureSource;
import com.automotive.costing.costingvariant.service.CostingVariantService;
import com.automotive.costing.masterdata.entity.LaborMaster;
import com.automotive.costing.masterdata.entity.MachineMaster;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.entity.MaterialMaster;
import com.automotive.costing.masterdata.repository.LaborMasterRepository;
import com.automotive.costing.masterdata.repository.MachineMasterRepository;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.material.engine.MaterialCostEngine;
import com.automotive.costing.material.engine.MaterialCostEngine.MaterialCostInput;
import com.automotive.costing.material.engine.MaterialCostEngine.MaterialCostResult;
import com.automotive.costing.material.entity.PricingBasis;
import com.automotive.costing.process.engine.ProcessCostEngine;
import com.automotive.costing.process.engine.ProcessCostEngine.OperationCostInput;
import com.automotive.costing.process.engine.ProcessCostEngine.OperationCostResult;
import com.automotive.costing.process.engine.ProcessCostEngine.RoutingCostResult;
import com.automotive.costing.process.entity.ProcessOperation;
import com.automotive.costing.process.entity.ProcessRouting;
import com.automotive.costing.process.repository.ProcessOperationRepository;
import com.automotive.costing.process.repository.ProcessRoutingRepository;
import com.automotive.costing.quantitystructure.dto.QuantityStructureDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A2 Automatic Quantity-Structure Costing.
 * BOM node + process routing (+ optional costing variant) → material + process estimate
 * using existing MaterialCostEngine / ProcessCostEngine. Does not create a second costing engine.
 */
@Service
@RequiredArgsConstructor
public class QuantityStructureCostingService {

    private static final int SCALE = 6;
    private static final BigDecimal SECONDS_PER_HOUR = new BigDecimal("3600");

    private final BomNodeRepository bomNodeRepository;
    private final ProcessRoutingRepository routingRepository;
    private final ProcessOperationRepository operationRepository;
    private final MachineMasterRepository machineMasterRepository;
    private final LaborMasterRepository laborMasterRepository;
    private final MaterialMasterRepository materialMasterRepository;
    private final CostingVariantService costingVariantService;

    @Transactional(readOnly = true)
    public EstimateResponse estimate(EstimateRequest request) {
        BomNode node = bomNodeRepository.findByBomNodeId(request.bomNodeId())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", request.bomNodeId()));

        CostingVariant variant = null;
        BigDecimal lotSize = BigDecimal.ONE;
        BigDecimal scrapPct = node.getScrapPct() != null ? node.getScrapPct() : BigDecimal.ZERO;
        BigDecimal yieldPct = BigDecimal.ONE;
        String qsSource = QuantityStructureSource.BOM_AND_ROUTING.name();
        String variantCode = null;

        if (request.costingVariantId() != null && !request.costingVariantId().isBlank()) {
            variant = costingVariantService.requireActiveForCosting(request.costingVariantId().trim());
            variantCode = variant.getVariantCode();
            lotSize = variant.getLotSize() != null ? variant.getLotSize() : BigDecimal.ONE;
            if (variant.getScrapPct() != null) {
                scrapPct = variant.getScrapPct();
            }
            if (variant.getYieldPct() != null) {
                yieldPct = variant.getYieldPct();
            }
            if (variant.getQuantityStructureSource() != null) {
                qsSource = variant.getQuantityStructureSource().name();
            }
        }
        if (request.lotSize() != null) {
            if (request.lotSize().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleViolationException("lotSize must be > 0", "QS_LOT_SIZE");
            }
            lotSize = request.lotSize().setScale(SCALE, RoundingMode.HALF_UP);
        }

        ProcessRouting routing = resolveRouting(request.routingId(), node.getBomNodeId());
        List<ProcessOperation> operations = operationRepository
                .findByRoutingIdOrderBySequenceNumberAsc(routing.getRoutingId());
        if (operations.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Routing has no operations — quantity-structure process cost cannot be computed",
                    "QS_NO_OPERATIONS");
        }

        // Process: apply lot size as setup batch size when operation has setup time
        List<OperationCostInput> inputs = new ArrayList<>();
        for (ProcessOperation op : operations) {
            BigDecimal batch = lotSize;
            if (op.getSetupTimeSeconds() == null) {
                batch = op.getBatchSize(); // may be null; engine handles
            } else if (op.getBatchSize() != null && request.lotSize() == null && variant == null) {
                batch = op.getBatchSize();
            }
            inputs.add(new OperationCostInput(
                    op.getOperationId(),
                    op.getCycleTimeSeconds(),
                    resolveMachineRate(op.getMachineCode()),
                    resolveLaborRate(op.getLaborCode()),
                    op.getOperatorCount(),
                    op.getSetupTimeSeconds(),
                    batch,
                    op.getOutputPartsPerCycle()
            ));
        }
        RoutingCostResult processResult = ProcessCostEngine.calculateRouting(inputs);

        List<OperationLine> opLines = new ArrayList<>();
        for (int i = 0; i < operations.size(); i++) {
            ProcessOperation op = operations.get(i);
            OperationCostResult r = processResult.operationResults().get(i);
            BigDecimal cycleH = op.getCycleTimeSeconds().divide(SECONDS_PER_HOUR, 10, RoundingMode.HALF_UP);
            BigDecimal setupH = op.getSetupTimeSeconds() == null
                    ? BigDecimal.ZERO
                    : op.getSetupTimeSeconds().divide(SECONDS_PER_HOUR, 10, RoundingMode.HALF_UP);
            BigDecimal batchUsed = inputs.get(i).batchSize() != null ? inputs.get(i).batchSize() : BigDecimal.ONE;
            BigDecimal laborHours = cycleH.multiply(op.getOperatorCount() != null ? op.getOperatorCount() : BigDecimal.ONE)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            opLines.add(new OperationLine(
                    op.getOperationId(),
                    op.getSequenceNumber(),
                    op.getProcessName(),
                    op.getMachineCode(),
                    op.getLaborCode(),
                    op.getCycleTimeSeconds(),
                    op.getSetupTimeSeconds(),
                    batchUsed.setScale(SCALE, RoundingMode.HALF_UP),
                    cycleH.setScale(SCALE, RoundingMode.HALF_UP),
                    laborHours,
                    r.runCost(),
                    r.setupCostPerPiece(),
                    r.totalOperationCost()
            ));
        }

        MaterialLine materialLine = buildMaterialLine(node, scrapPct, yieldPct);
        BigDecimal matTotal = materialLine != null && materialLine.materialCost() != null
                ? materialLine.materialCost() : BigDecimal.ZERO;
        BigDecimal procTotal = processResult.totalProcessCost();
        BigDecimal total = matTotal.add(procTotal).setScale(SCALE, RoundingMode.HALF_UP);

        String basis = "A2 QS: source=" + qsSource
                + "; lot=" + lotSize.toPlainString()
                + "; scrap=" + scrapPct.toPlainString()
                + "; yield=" + yieldPct.toPlainString()
                + "; routing=" + routing.getRoutingId()
                + (variantCode != null ? "; variant=" + variantCode : "")
                + " | process: " + processResult.calculationBasis()
                + (materialLine != null ? " | material: " + materialLine.basis() : " | material: none");
        if (basis.length() > 2000) {
            basis = basis.substring(0, 2000);
        }

        return new EstimateResponse(
                node.getBomNodeId(),
                routing.getRoutingId(),
                variant != null ? variant.getVariantId() : null,
                variantCode,
                lotSize.setScale(SCALE, RoundingMode.HALF_UP),
                scrapPct.setScale(SCALE, RoundingMode.HALF_UP),
                yieldPct.setScale(SCALE, RoundingMode.HALF_UP),
                qsSource,
                materialLine,
                opLines,
                matTotal.setScale(SCALE, RoundingMode.HALF_UP),
                procTotal,
                total,
                basis
        );
    }

    private ProcessRouting resolveRouting(String routingId, String bomNodeId) {
        if (routingId != null && !routingId.isBlank()) {
            ProcessRouting r = routingRepository.findByRoutingId(routingId.trim())
                    .orElseThrow(() -> ResourceNotFoundException.of("ProcessRouting", routingId));
            if (!bomNodeId.equals(r.getBomNodeId())) {
                throw new BusinessRuleViolationException(
                        "Routing does not belong to BOM node", "QS_ROUTING_BOM_MISMATCH");
            }
            return r;
        }
        List<ProcessRouting> list = routingRepository.findByBomNodeId(bomNodeId);
        if (list.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No process routing on BOM node — create a routing before quantity-structure costing",
                    "QS_NO_ROUTING");
        }
        // Prefer most recently created (id order is weak); list as returned
        return list.stream()
                .max(Comparator.comparing(ProcessRouting::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(list.get(0));
    }

    private MaterialLine buildMaterialLine(BomNode node, BigDecimal scrapPct, BigDecimal yieldPct) {
        if (node.getMaterialCode() == null || node.getMaterialCode().isBlank()) {
            return null;
        }
        MaterialMaster material = materialMasterRepository
                .findFirstByCodeOrderByVersionNumberDesc(node.getMaterialCode())
                .orElse(null);
        if (material == null || material.getBasePrice() == null) {
            return new MaterialLine(
                    node.getMaterialCode(),
                    node.getNetWeight(),
                    scrapPct,
                    yieldPct,
                    null,
                    null,
                    null,
                    "MATERIAL_RATE_UNAVAILABLE"
            );
        }
        BigDecimal net = node.getNetWeight();
        if (net == null || net.compareTo(BigDecimal.ZERO) <= 0) {
            // Fall back to quantity_per as net quantity for unit costing
            net = node.getQuantityPer() != null ? node.getQuantityPer() : BigDecimal.ONE;
        }
        // Yield: effective net = net / yield (more material needed if yield < 1)
        if (yieldPct.compareTo(BigDecimal.ZERO) > 0 && yieldPct.compareTo(BigDecimal.ONE) < 0) {
            net = net.divide(yieldPct, 10, RoundingMode.HALF_UP);
        }
        MaterialCostResult result = MaterialCostEngine.calculate(new MaterialCostInput(
                net,
                scrapPct,
                material.getBasePrice(),
                PricingBasis.FIXED,
                null,
                null
        ));
        return new MaterialLine(
                node.getMaterialCode(),
                net.setScale(SCALE, RoundingMode.HALF_UP),
                scrapPct.setScale(SCALE, RoundingMode.HALF_UP),
                yieldPct.setScale(SCALE, RoundingMode.HALF_UP),
                result.grossWeight(),
                material.getBasePrice(),
                result.materialCost(),
                result.calculationBasis()
        );
    }

    private BigDecimal resolveMachineRate(String machineCode) {
        if (machineCode == null || machineCode.isBlank()) {
            return null;
        }
        MachineMaster machine = machineMasterRepository
                .findFirstByCodeAndStatusOrderByVersionNumberDesc(machineCode, MasterDataStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Machine '" + machineCode + "' has no Active rate", "QS_MACHINE_RATE"));
        return machine.getRate();
    }

    private BigDecimal resolveLaborRate(String laborCode) {
        if (laborCode == null || laborCode.isBlank()) {
            return null;
        }
        LaborMaster labor = laborMasterRepository
                .findFirstByCodeAndStatusOrderByVersionNumberDesc(laborCode, MasterDataStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Labor '" + laborCode + "' has no Active rate", "QS_LABOR_RATE"));
        return labor.getRate();
    }
}
