package com.automotive.costing.reports.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Report response contracts for operational analytics.
 * All monetary fields use BigDecimal; never float/double.
 */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record MasterDataHealthReport(
            long materialTotal,
            long materialActive,
            long machineTotal,
            long machineActive,
            long laborTotal,
            long laborActive,
            long plantTotal,
            long programTotal
    ) {
    }

    public record ErpStagingReport(
            long totalBatches,
            Map<String, Long> byStatus
    ) {
    }

    public record ShouldCostElementReport(
            long buildUpCount,
            BigDecimal materialCostTotal,
            BigDecimal processCostTotal,
            BigDecimal toolingCostTotal,
            BigDecimal packagingFreightCostTotal,
            BigDecimal overheadCostTotal,
            BigDecimal sgaCostTotal,
            BigDecimal profitMarginTotal,
            BigDecimal grandTotal
    ) {
    }

    public record TargetVarianceReport(
            long varianceSnapshots,
            long exceedingThreshold,
            long withinThreshold,
            BigDecimal sumAbsoluteVariance,
            BigDecimal avgPercentageVariance
    ) {
    }

    /**
     * Composite operational report. When {@code programId} is non-null, program-scoped
     * metrics (BOM, should-cost, target variance, active targets) are filtered; master-data
     * and ERP staging remain system-wide (they are not program-owned).
     */
    public record OperationalReport(
            String programId,
            MasterDataHealthReport masterData,
            ErpStagingReport erpStaging,
            ShouldCostElementReport shouldCostElements,
            TargetVarianceReport targetVariance,
            long bomNodeCount,
            long packagingProfileCount,
            long activeTargetCosts,
            long pendingApprovalSteps,
            long notificationCount
    ) {
    }
}
