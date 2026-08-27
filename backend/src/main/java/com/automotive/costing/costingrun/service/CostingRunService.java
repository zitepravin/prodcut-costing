package com.automotive.costing.costingrun.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.costingrun.dto.CostingRunDtos.*;
import com.automotive.costing.costingrun.entity.*;
import com.automotive.costing.costingrun.repository.CostingRunItemRepository;
import com.automotive.costing.costingrun.repository.CostingRunRepository;
import com.automotive.costing.quantitystructure.dto.QuantityStructureDtos.EstimateRequest;
import com.automotive.costing.quantitystructure.dto.QuantityStructureDtos.EstimateResponse;
import com.automotive.costing.quantitystructure.service.QuantityStructureCostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * B1 Costing Run — mass quantity-structure estimates across selected BOM nodes.
 * Reuses QuantityStructureCostingService; does not invent rates.
 */
@Service
@RequiredArgsConstructor
public class CostingRunService {

    private final CostingRunRepository runRepository;
    private final CostingRunItemRepository itemRepository;
    private final BomNodeRepository bomNodeRepository;
    private final QuantityStructureCostingService quantityStructureCostingService;

    @Transactional(readOnly = true)
    public List<RunResponse> list() {
        return runRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toRun).toList();
    }

    @Transactional(readOnly = true)
    public RunDetailResponse get(String runId) {
        CostingRun run = require(runId);
        List<ItemResponse> items = itemRepository.findByRunIdOrderByPartNumberAsc(runId).stream()
                .map(this::toItem).toList();
        return new RunDetailResponse(toRun(run), items);
    }

    @Transactional
    public RunDetailResponse create(CreateRunRequest req) {
        runRepository.findByRunCode(req.runCode().trim()).ifPresent(r -> {
            throw new BusinessRuleViolationException("Run code exists: " + req.runCode(), "CR_CODE_EXISTS");
        });

        List<BomNode> nodes = resolveNodes(req);
        if (nodes.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No BOM nodes selected for costing run", "CR_NO_NODES");
        }

        CostingRun run = new CostingRun();
        run.setRunId(UUID.randomUUID().toString());
        run.setRunCode(req.runCode().trim());
        run.setDescription(req.description());
        run.setStatus(CostingRunStatus.DRAFT);
        run.setProgramId(trimToNull(req.programId()));
        run.setPlantCode(trimToNull(req.plantCode()));
        run.setCostingVariantId(trimToNull(req.costingVariantId()));
        run.setCostingSheetId(trimToNull(req.costingSheetId()));
        run.setLotSize(req.lotSize());
        run.setCostingDate(req.costingDate());
        runRepository.save(run);

        List<CostingRunItem> items = new ArrayList<>();
        for (BomNode n : nodes) {
            CostingRunItem item = new CostingRunItem();
            item.setItemId(UUID.randomUUID().toString());
            item.setRunId(run.getRunId());
            item.setBomNodeId(n.getBomNodeId());
            item.setPartNumber(n.getPartNumber());
            item.setStatus(CostingRunItemStatus.PENDING);
            items.add(item);
        }
        itemRepository.saveAll(items);
        return get(run.getRunId());
    }

    @Transactional
    public RunDetailResponse execute(String runId) {
        CostingRun run = require(runId);
        if (run.getStatus() == CostingRunStatus.RUNNING) {
            throw new BusinessRuleViolationException("Run already executing", "CR_RUNNING");
        }
        run.setStatus(CostingRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run.setSuccessCount(0);
        run.setFailureCount(0);
        runRepository.save(run);

        List<CostingRunItem> items = itemRepository.findByRunIdOrderByPartNumberAsc(runId);
        int ok = 0, fail = 0;
        for (CostingRunItem item : items) {
            if (item.getStatus() == CostingRunItemStatus.SUCCESS) {
                ok++;
                continue; // skip already successful on rerun
            }
            try {
                EstimateResponse est = quantityStructureCostingService.estimate(new EstimateRequest(
                        item.getBomNodeId(),
                        run.getCostingVariantId(),
                        null,
                        run.getLotSize()
                ));
                item.setStatus(CostingRunItemStatus.SUCCESS);
                item.setUnitCost(est.totalQuantityStructureCost());
                item.setErrorMessage(null);
                String basis = est.calculationBasis();
                if (basis != null && basis.length() > 2000) {
                    basis = basis.substring(0, 2000);
                }
                item.setEstimateBasis(basis);
                item.setCalculatedAt(Instant.now());
                ok++;
            } catch (Exception ex) {
                item.setStatus(CostingRunItemStatus.FAILED);
                item.setUnitCost(null);
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                if (msg.length() > 1000) msg = msg.substring(0, 1000);
                item.setErrorMessage(msg);
                item.setCalculatedAt(Instant.now());
                fail++;
            }
            itemRepository.save(item);
        }

        run.setSuccessCount(ok);
        run.setFailureCount(fail);
        run.setCompletedAt(Instant.now());
        if (fail == 0) {
            run.setStatus(CostingRunStatus.COMPLETED);
        } else if (ok == 0) {
            run.setStatus(CostingRunStatus.FAILED);
        } else {
            run.setStatus(CostingRunStatus.COMPLETED_WITH_ERRORS);
        }
        runRepository.save(run);
        return get(runId);
    }

    /** Rerun only FAILED items. */
    @Transactional
    public RunDetailResponse rerunFailed(String runId) {
        CostingRun run = require(runId);
        List<CostingRunItem> failed = itemRepository.findByRunIdAndStatus(runId, CostingRunItemStatus.FAILED);
        for (CostingRunItem item : failed) {
            item.setStatus(CostingRunItemStatus.PENDING);
            item.setErrorMessage(null);
            itemRepository.save(item);
        }
        return execute(run.getRunId());
    }

    private List<BomNode> resolveNodes(CreateRunRequest req) {
        if (req.bomNodeIds() != null && !req.bomNodeIds().isEmpty()) {
            List<BomNode> list = new ArrayList<>();
            for (String id : req.bomNodeIds()) {
                BomNode n = bomNodeRepository.findByBomNodeId(id.trim())
                        .orElseThrow(() -> ResourceNotFoundException.of("BomNode", id));
                list.add(n);
            }
            return list;
        }
        if (req.programId() != null && !req.programId().isBlank()) {
            return bomNodeRepository.findFullTreeByProgram(req.programId().trim());
        }
        return List.of();
    }

    private CostingRun require(String id) {
        return runRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CostingRun", id));
    }

    private RunResponse toRun(CostingRun r) {
        return new RunResponse(
                r.getRunId(), r.getRunCode(), r.getDescription(), r.getStatus(),
                r.getProgramId(), r.getPlantCode(), r.getCostingVariantId(), r.getCostingSheetId(),
                r.getLotSize(), r.getCostingDate(),
                r.getSuccessCount(), r.getFailureCount(),
                r.getStartedAt(), r.getCompletedAt()
        );
    }

    private ItemResponse toItem(CostingRunItem i) {
        return new ItemResponse(
                i.getItemId(), i.getBomNodeId(), i.getPartNumber(), i.getStatus(),
                i.getUnitCost(), i.getErrorMessage(), i.getEstimateBasis(), i.getCalculatedAt()
        );
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
