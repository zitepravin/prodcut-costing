package com.automotive.costing.variance.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.targetcosting.entity.TargetCostRecord;
import com.automotive.costing.targetcosting.repository.TargetCostRecordRepository;
import com.automotive.costing.variance.dto.VarianceDtos.*;
import com.automotive.costing.variance.engine.UnifiedVarianceEngine;
import com.automotive.costing.variance.engine.UnifiedVarianceEngine.CostSnapshot;
import com.automotive.costing.variance.engine.UnifiedVarianceEngine.VarianceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * B3 Unified variance — should-cost vs target / quote / previous using component drivers.
 * Does not invent actual costs.
 */
@Service
@RequiredArgsConstructor
public class UnifiedVarianceService {

    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    private final TargetCostRecordRepository targetCostRecordRepository;

    @Transactional(readOnly = true)
    public CompareResponse compare(CompareRequest req) {
        CostSnapshot baseline = resolve(req.bomNodeId(), req.baselineType(), req.baselineId(), null);
        CostSnapshot comparison = resolve(req.bomNodeId(), req.comparisonType(), req.comparisonId(), req.comparisonTotal());

        VarianceResult result = UnifiedVarianceEngine.compare(baseline, comparison, req.comparisonType());
        List<DriverResponse> drivers = result.drivers().stream()
                .map(d -> new DriverResponse(
                        d.driverCode(), d.driverName(), d.baselineAmount(), d.comparisonAmount(),
                        d.absoluteVariance(), d.percentVariance(), d.explanation()))
                .toList();

        return new CompareResponse(
                req.bomNodeId(),
                req.baselineType(),
                req.comparisonType(),
                result.baselineTotal(),
                result.comparisonTotal(),
                result.absoluteVariance(),
                result.percentVariance(),
                drivers,
                result.summary()
        );
    }

    private CostSnapshot resolve(String bomNodeId, String type, String id, BigDecimal adHocTotal) {
        String t = type.trim().toUpperCase();
        return switch (t) {
            case "SHOULD_COST" -> fromShouldCost(bomNodeId, id, false);
            case "PREVIOUS_SHOULD_COST" -> fromShouldCost(bomNodeId, id, true);
            case "TARGET" -> fromTarget(bomNodeId, id);
            case "QUOTE", "SUPPLIER_QUOTE" -> {
                if (adHocTotal == null && (id == null || id.isBlank())) {
                    throw new BusinessRuleViolationException(
                            "QUOTE comparison requires comparisonTotal or comparisonId", "VAR_QUOTE_TOTAL");
                }
                yield CostSnapshot.ofTotal(adHocTotal != null ? adHocTotal : BigDecimal.ZERO);
            }
            case "ADHOC" -> {
                if (adHocTotal == null) {
                    throw new BusinessRuleViolationException("ADHOC requires comparisonTotal", "VAR_ADHOC");
                }
                yield CostSnapshot.ofTotal(adHocTotal);
            }
            default -> throw new BusinessRuleViolationException("Unknown type: " + type, "VAR_TYPE");
        };
    }

    private CostSnapshot fromShouldCost(String bomNodeId, String id, boolean previous) {
        ShouldCostBuildUp sc;
        if (id != null && !id.isBlank()) {
            sc = shouldCostBuildUpRepository.findById(id)
                    .orElseThrow(() -> ResourceNotFoundException.of("ShouldCostBuildUp", id));
        } else {
            List<ShouldCostBuildUp> list = shouldCostBuildUpRepository
                    .findByBomNodeIdOrderByCalculatedAtDesc(bomNodeId);
            if (list.isEmpty()) {
                throw new ResourceNotFoundException("No should-cost for BOM node " + bomNodeId);
            }
            int idx = previous ? 1 : 0;
            if (idx >= list.size()) {
                throw new BusinessRuleViolationException(
                        "No previous should-cost version for BOM node", "VAR_NO_PREVIOUS");
            }
            sc = list.get(idx);
        }
        return new CostSnapshot(
                sc.getMaterialCost(), sc.getProcessCost(), sc.getToolingCost(),
                sc.getPackagingFreightCost(), sc.getOverheadCost(),
                sc.getSgaCost(), sc.getProfitMargin(), sc.getTotalCost()
        );
    }

    private CostSnapshot fromTarget(String bomNodeId, String id) {
        TargetCostRecord t;
        if (id != null && !id.isBlank()) {
            t = targetCostRecordRepository.findById(id)
                    .orElseThrow(() -> ResourceNotFoundException.of("TargetCostRecord", id));
        } else {
            // try common finder
            t = targetCostRecordRepository.findByBomNodeIdOrderByEffectiveFromDesc(bomNodeId).stream()
                    .findFirst()
                    .orElseThrow(() -> ResourceNotFoundException.of("TargetCostRecord", bomNodeId));
        }
        return CostSnapshot.ofTotal(t.getTargetAmount());
    }
}
