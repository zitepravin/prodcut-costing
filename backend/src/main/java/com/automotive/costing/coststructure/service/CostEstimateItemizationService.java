package com.automotive.costing.coststructure.service;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.coststructure.dto.CostStructureDtos.ItemizationLine;
import com.automotive.costing.coststructure.dto.CostStructureDtos.ItemizationResponse;
import com.automotive.costing.coststructure.entity.CostEstimateItem;
import com.automotive.costing.coststructure.repository.CostEstimateItemRepository;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A5 Cost Estimate Itemization — Qty × Rate × Source × Component → Amount.
 * Builds lines from a should-cost build-up and persists for audit/drill-down.
 */
@Service
@RequiredArgsConstructor
public class CostEstimateItemizationService {

    public static final String REF_SHOULD_COST = "SHOULD_COST";
    private static final int SCALE = 6;

    private final CostEstimateItemRepository itemRepository;
    private final ShouldCostBuildUpRepository buildUpRepository;

    @Transactional
    public ItemizationResponse materializeFromShouldCost(String buildUpId) {
        ShouldCostBuildUp sc = buildUpRepository.findById(buildUpId)
                .orElseThrow(() -> ResourceNotFoundException.of("ShouldCostBuildUp", buildUpId));

        itemRepository.deleteByEstimateRefTypeAndEstimateRefId(REF_SHOULD_COST, buildUpId);

        List<CostEstimateItem> items = new ArrayList<>();
        int line = 10;
        items.add(line(sc, line, "MATERIAL", "Material", sc.getMaterialCost(), "VARIABLE",
                "MATERIAL_MASTER_OR_CALC", "maps_to material_cost"));
        line += 10;
        items.add(line(sc, line, "PROCESS", "Process / conversion", sc.getProcessCost(), "VARIABLE",
                "ROUTING_RATES", "maps_to process_cost"));
        line += 10;
        items.add(line(sc, line, "TOOLING", "Tooling amortization", sc.getToolingCost(), "FIXED",
                "TOOLING_RECORD", "maps_to tooling_cost"));
        line += 10;
        items.add(line(sc, line, "PACKAGING_FREIGHT", "Packaging & freight", sc.getPackagingFreightCost(), "VARIABLE",
                "PACKAGING_CALC", "maps_to packaging_freight_cost"));
        line += 10;
        items.add(line(sc, line, "OVERHEAD", "Overhead", sc.getOverheadCost(), "FIXED",
                "OVERHEAD_POOL", "maps_to overhead_cost"));
        line += 10;
        items.add(line(sc, line, "SGA", "SG&A", sc.getSgaCost(), "VARIABLE",
                "SGA_BENCHMARK", "maps_to sga_cost"));
        line += 10;
        items.add(line(sc, line, "PROFIT_MARGIN", "Profit margin", sc.getProfitMargin(), "VARIABLE",
                "MARGIN_BENCHMARK", "maps_to profit_margin"));

        itemRepository.saveAll(items);
        return toResponse(REF_SHOULD_COST, buildUpId, sc.getBomNodeId(), items);
    }

    @Transactional(readOnly = true)
    public ItemizationResponse get(String refType, String refId) {
        List<CostEstimateItem> items = itemRepository
                .findByEstimateRefTypeAndEstimateRefIdOrderByLineNoAsc(refType, refId);
        String bomNodeId = items.isEmpty() ? null : items.get(0).getBomNodeId();
        return toResponse(refType, refId, bomNodeId, items);
    }

    private CostEstimateItem line(
            ShouldCostBuildUp sc, int lineNo, String code, String desc, BigDecimal amount,
            String behavior, String source, String rule) {
        BigDecimal amt = amount == null ? BigDecimal.ZERO : amount.setScale(SCALE, RoundingMode.HALF_UP);
        CostEstimateItem item = new CostEstimateItem();
        item.setItemId(UUID.randomUUID().toString());
        item.setEstimateRefType(REF_SHOULD_COST);
        item.setEstimateRefId(sc.getBuildUpId());
        item.setBomNodeId(sc.getBomNodeId());
        item.setLineNo(lineNo);
        item.setComponentCode(code);
        item.setDescription(desc);
        item.setQuantity(BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP));
        item.setUnitOfMeasure("EA");
        item.setRate(amt);
        item.setAmount(amt);
        item.setCurrencyCode("USD");
        item.setCostBehavior(behavior);
        item.setPriceSource(source);
        item.setCalculationRule(rule);
        item.setCreatedAt(Instant.now());
        return item;
    }

    private ItemizationResponse toResponse(String type, String refId, String bomNodeId, List<CostEstimateItem> items) {
        List<ItemizationLine> lines = items.stream()
                .map(i -> new ItemizationLine(
                        i.getItemId(), i.getLineNo(), i.getComponentCode(), i.getDescription(),
                        i.getQuantity(), i.getUnitOfMeasure(), i.getRate(), i.getAmount(),
                        i.getCurrencyCode(), i.getCostBehavior(), i.getPriceSource(), i.getCalculationRule()))
                .toList();
        BigDecimal total = items.stream().map(CostEstimateItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fixed = items.stream()
                .filter(i -> "FIXED".equalsIgnoreCase(i.getCostBehavior()))
                .map(CostEstimateItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variable = items.stream()
                .filter(i -> !"FIXED".equalsIgnoreCase(i.getCostBehavior()))
                .map(CostEstimateItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ItemizationResponse(type, refId, bomNodeId, lines, total, fixed, variable);
    }
}
