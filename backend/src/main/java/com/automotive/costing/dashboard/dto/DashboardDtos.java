package com.automotive.costing.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    /**
     * High-level operational KPIs for the home dashboard.
     * Counts are tenant-scoped via CustomerContext. Permission-gated fields are 0 / 0.00
     * when the caller lacks the catalog permission (never invented business values).
     */
    public record KpiResponse(
            long activePrograms,
            long materialMasters,
            long bomNodes,
            long shouldCostBuildUps,
            long packagingProfiles,
            long activeTargetCosts,
            long pendingApprovalSteps,
            long targetVariancesExceedingThreshold,
            long unreadNotifications,
            long openSupplierRisks,
            long pendingPolicyEvidence,
            long openWarrantyClaims,
            long unresolvedGuaranteeEvents,
            long pendingPerformanceFeedback,
            long draftRecommendations,
            long submittedRecommendations,
            long expiringCapabilities,
            long openSupportTickets,
            long inProgressSupportTickets,
            long waitingCustomerSupportTickets,
            long resolvedSupportTickets,
            long openRfqs,
            long rfqsClosingSoon,
            long awaitingQuotations,
            long openPurchaseOrders,
            long overduePurchaseOrders,
            long openPurchaseRequests,
            long buyerOpenOrders,
            BigDecimal purchasingSpend,
            long incompleteMaterialMasters,
            long draftRoutings,
            long routingMissingAssignments,
            long cycleTimeExceptions,
            long activeToolingRecords,
            BigDecimal toolingInvestment
    ) {
    }

    public enum PositionAmountStatus {
        AVAILABLE,
        NOT_AVAILABLE,
        PARTIAL
    }

    public record DriverAmountDto(
            String componentCode,
            String displayName,
            BigDecimal amount,
            String status
    ) {
    }

    public record CoverageDto(
            long bomNodesInProgram,
            long nodesWithSelectedShouldCost,
            long nodesMissingShouldCost,
            long nodesWithCurrencyGap,
            long buildUpsConsideredBeforeSelection,
            BigDecimal coverageRatio
    ) {
    }

    public record ProgramCostPositionResponse(
            String programId,
            String programCode,
            String modelName,
            String reportingCurrency,
            Instant asOf,
            String selectionPolicy,
            BigDecimal shouldCostTotal,
            String shouldCostStatus,
            BigDecimal targetTotal,
            String targetStatus,
            String targetSource,
            BigDecimal absoluteVariance,
            BigDecimal percentageVariance,
            String varianceStatus,
            CoverageDto coverage,
            List<DriverAmountDto> drivers,
            List<String> dataQualityNotes
    ) {
    }
}
