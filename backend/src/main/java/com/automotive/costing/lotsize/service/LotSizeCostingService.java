package com.automotive.costing.lotsize.service;

import com.automotive.costing.lotsize.dto.LotSizeDtos.*;
import com.automotive.costing.quantitystructure.dto.QuantityStructureDtos.EstimateRequest;
import com.automotive.costing.quantitystructure.dto.QuantityStructureDtos.EstimateResponse;
import com.automotive.costing.quantitystructure.service.QuantityStructureCostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * B2 Lot-size costing — unit cost curve across standard volumes.
 * Reuses A2 quantity-structure engine.
 */
@Service
@RequiredArgsConstructor
public class LotSizeCostingService {

    private static final List<BigDecimal> DEFAULT_LOTS = List.of(
            BigDecimal.ONE,
            new BigDecimal("10"),
            new BigDecimal("100"),
            new BigDecimal("1000"),
            new BigDecimal("10000"),
            new BigDecimal("100000")
    );

    private final QuantityStructureCostingService quantityStructureCostingService;

    @Transactional(readOnly = true)
    public CurveResponse curve(CurveRequest request) {
        List<BigDecimal> lots = (request.lotSizes() == null || request.lotSizes().isEmpty())
                ? DEFAULT_LOTS : request.lotSizes();

        List<CurvePoint> points = new ArrayList<>();
        BigDecimal bestLot = null;
        BigDecimal bestCost = null;

        for (BigDecimal lot : lots) {
            if (lot == null || lot.compareTo(BigDecimal.ZERO) <= 0) {
                points.add(new CurvePoint(lot, null, null, null, "SKIPPED", "lot size must be > 0"));
                continue;
            }
            try {
                EstimateResponse est = quantityStructureCostingService.estimate(new EstimateRequest(
                        request.bomNodeId(),
                        request.costingVariantId(),
                        request.routingId(),
                        lot
                ));
                points.add(new CurvePoint(
                        lot,
                        est.totalQuantityStructureCost(),
                        est.totalMaterialCost(),
                        est.totalProcessCost(),
                        "OK",
                        null
                ));
                if (bestCost == null || est.totalQuantityStructureCost().compareTo(bestCost) < 0) {
                    bestCost = est.totalQuantityStructureCost();
                    bestLot = lot;
                }
            } catch (Exception ex) {
                points.add(new CurvePoint(lot, null, null, null, "ERROR",
                        ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
            }
        }

        return new CurveResponse(
                request.bomNodeId(),
                points,
                bestLot,
                "B2 lot-size curve via quantity-structure engine; setup amortization falls with larger lots"
        );
    }
}
