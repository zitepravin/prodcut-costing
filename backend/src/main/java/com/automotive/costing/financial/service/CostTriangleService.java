package com.automotive.costing.financial.service;

import com.automotive.costing.erp.sap.entity.SapCostEstimateStaging;
import com.automotive.costing.erp.sap.repository.SapCostEstimateStagingRepository;
import com.automotive.costing.financial.dto.FinancialDtos.BucketAmount;
import com.automotive.costing.financial.dto.FinancialDtos.TriangleCompareResponse;
import com.automotive.costing.financial.entity.ActualCostRecord;
import com.automotive.costing.financial.repository.ActualCostRecordRepository;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.standardcost.entity.StandardCost;
import com.automotive.costing.standardcost.entity.StandardCostStatus;
import com.automotive.costing.standardcost.repository.StandardCostRepository;
import com.automotive.costing.targetcosting.entity.TargetCostRecord;
import com.automotive.costing.targetcosting.repository.TargetCostRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * C2/C3 — Planned / Standard / Should / Actual / SAP triangle.
 * Missing buckets stay DATA_NOT_AVAILABLE — never invent actuals.
 * SAP staging is comparison only (no Material Ledger).
 */
@Service
@RequiredArgsConstructor
public class CostTriangleService {

    private static final int SCALE = 6;

    private final StandardCostRepository standardCostRepository;
    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    private final ActualCostRecordRepository actualCostRecordRepository;
    private final TargetCostRecordRepository targetCostRecordRepository;
    private final SapCostEstimateStagingRepository sapStagingRepository;

    @Transactional(readOnly = true)
    public TriangleCompareResponse compare(String bomNodeId, String reportingCurrency) {
        String currency = reportingCurrency == null || reportingCurrency.isBlank() ? "USD" : reportingCurrency.toUpperCase();
        List<BucketAmount> buckets = new ArrayList<>();

        // PLANNED = target cost
        Optional<TargetCostRecord> target = targetCostRecordRepository
                .findByBomNodeIdOrderByEffectiveFromDesc(bomNodeId).stream().findFirst();
        if (target.isPresent()) {
            buckets.add(ok("PLANNED", target.get().getTargetAmount(), target.get().getCurrencyCode(),
                    target.get().getTargetCostId(), "Target cost record"));
        } else {
            buckets.add(missing("PLANNED", "No target cost for BOM node"));
        }

        // STANDARD = released or frozen standard cost
        Optional<StandardCost> std = standardCostRepository
                .findFirstByBomNodeIdAndStatusOrderByVersionNoDesc(bomNodeId, StandardCostStatus.RELEASED)
                .or(() -> standardCostRepository.findFirstByBomNodeIdAndStatusOrderByVersionNoDesc(
                        bomNodeId, StandardCostStatus.FROZEN));
        if (std.isPresent()) {
            buckets.add(ok("STANDARD", std.get().getUnitCost(), std.get().getCurrencyCode(),
                    std.get().getStandardCostId(), "Released/frozen standard cost v" + std.get().getVersionNo()));
        } else {
            buckets.add(missing("STANDARD", "No RELEASED/FROZEN standard cost"));
        }

        // SHOULD
        Optional<ShouldCostBuildUp> sc = shouldCostBuildUpRepository
                .findByBomNodeIdOrderByCalculatedAtDesc(bomNodeId).stream().findFirst();
        if (sc.isPresent() && sc.get().getTotalCost() != null) {
            buckets.add(ok("SHOULD", sc.get().getTotalCost(), "USD",
                    sc.get().getBuildUpId(), "Latest should-cost build-up"));
        } else {
            buckets.add(missing("SHOULD", "No should-cost build-up"));
        }

        // ACTUAL — import only
        Optional<ActualCostRecord> act = actualCostRecordRepository.findFirstByBomNodeIdOrderByImportedAtDesc(bomNodeId);
        if (act.isPresent()) {
            buckets.add(ok("ACTUAL", act.get().getUnitCost(), act.get().getCurrencyCode(),
                    act.get().getActualCostId(), "Imported actual (" + act.get().getSourceSystem() + ")"));
        } else {
            buckets.add(missing("ACTUAL", "No imported actual — actuals are never invented"));
        }

        // SAP staged estimate
        Optional<SapCostEstimateStaging> sap = sapStagingRepository
                .findByBomNodeIdOrderByReceivedAtDesc(bomNodeId).stream().findFirst();
        if (sap.isPresent() && sap.get().getTotalCost() != null) {
            buckets.add(ok("SAP", sap.get().getTotalCost(),
                    sap.get().getCurrencyCode() != null ? sap.get().getCurrencyCode() : "USD",
                    sap.get().getStagingId(), "SAP cost estimate staging (comparison only)"));
        } else {
            buckets.add(missing("SAP", "No SAP staged estimate linked to BOM node"));
        }

        BigDecimal planned = amountOf(buckets, "PLANNED");
        BigDecimal standard = amountOf(buckets, "STANDARD");
        BigDecimal should = amountOf(buckets, "SHOULD");
        BigDecimal actual = amountOf(buckets, "ACTUAL");

        BigDecimal stdVsShould = delta(standard, should);
        BigDecimal stdVsActual = delta(standard, actual);
        BigDecimal shouldVsActual = delta(should, actual);

        String summary = "C3 triangle: missing buckets marked DATA_NOT_AVAILABLE; "
                + "SAP is staged comparison only (no Material Ledger / FI-CO clone). "
                + "Reporting currency context=" + currency + " (amounts shown in source currency).";

        return new TriangleCompareResponse(
                bomNodeId, currency, LocalDate.now(), buckets,
                stdVsShould, stdVsActual, shouldVsActual, summary
        );
    }

    private static BucketAmount ok(String bucket, BigDecimal amt, String ccy, String ref, String notes) {
        return new BucketAmount(bucket,
                amt != null ? amt.setScale(SCALE, RoundingMode.HALF_UP) : null,
                ccy, ref, "OK", notes);
    }

    private static BucketAmount missing(String bucket, String notes) {
        return new BucketAmount(bucket, null, null, null, "DATA_NOT_AVAILABLE", notes);
    }

    private static BigDecimal amountOf(List<BucketAmount> buckets, String code) {
        return buckets.stream()
                .filter(b -> code.equals(b.bucket()) && "OK".equals(b.dataStatus()))
                .map(BucketAmount::unitCost)
                .findFirst()
                .orElse(null);
    }

    private static BigDecimal delta(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        return a.subtract(b).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
