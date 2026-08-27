package com.automotive.costing.dashboard.service;

import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.dashboard.dto.DashboardDtos.KpiResponse;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.notification.repository.AppNotificationRepository;
import com.automotive.costing.packaging.repository.PackagingLogisticsProfileRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.process.entity.RoutingStatus;
import com.automotive.costing.process.repository.ProcessOperationRepository;
import com.automotive.costing.process.repository.ProcessRoutingRepository;
import com.automotive.costing.purchase.repository.PurchaseOrderLineRepository;
import com.automotive.costing.purchase.repository.PurchaseOrderRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import com.automotive.costing.purchase.service.PurchaseOrderStatus;
import com.automotive.costing.rfq.entity.RfqStatus;
import com.automotive.costing.rfq.repository.RfqEventRepository;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.support.entity.SupportTicketStatus;
import com.automotive.costing.support.repository.SupportTicketRepository;
import com.automotive.costing.security.PermissionAuth;
import com.automotive.costing.targetcosting.entity.TargetCostStatus;
import com.automotive.costing.targetcosting.repository.TargetCostRecordRepository;
import com.automotive.costing.targetcosting.repository.TargetCostVarianceRepository;
import com.automotive.costing.tooling.entity.ToolingAssetStatus;
import com.automotive.costing.tooling.repository.ToolingRecordRepository;
import com.automotive.costing.vendorselection.entity.CapabilityStatus;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.RecommendationStatus;
import com.automotive.costing.vendorselection.entity.RiskStatus;
import com.automotive.costing.vendorselection.entity.WarrantyClaimStatus;
import com.automotive.costing.vendorselection.repository.SupplierCapabilityRepository;
import com.automotive.costing.vendorselection.repository.SupplierGuaranteeEventRepository;
import com.automotive.costing.vendorselection.repository.SupplierPerformanceFeedbackRepository;
import com.automotive.costing.vendorselection.repository.SupplierPolicyEvidenceRepository;
import com.automotive.costing.vendorselection.repository.SupplierRiskRepository;
import com.automotive.costing.vendorselection.repository.SupplierWarrantyClaimRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationRepository;
import com.automotive.costing.workflow.entity.DecisionStatus;
import com.automotive.costing.workflow.repository.ApprovalStepInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Dashboard KPI aggregates scoped to the current tenant (CustomerContext).
 * Lightweight count / sum queries only. Permission-gated slices return 0 when unauthorized.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RFQ_CLOSING_SOON_DAYS = 7;
    private static final Set<String> OPEN_PO_STATUSES = Set.of(
            PurchaseOrderStatus.DRAFT,
            PurchaseOrderStatus.SUBMITTED,
            PurchaseOrderStatus.APPROVAL,
            PurchaseOrderStatus.APPROVED,
            PurchaseOrderStatus.ISSUED,
            PurchaseOrderStatus.PARTIALLY_RECEIVED,
            PurchaseOrderStatus.REWORK);
    private static final Set<String> IN_FLIGHT_PO_STATUSES = Set.of(
            PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.PARTIALLY_RECEIVED);
    private static final Set<String> SPEND_PO_STATUSES = Set.of(
            PurchaseOrderStatus.ISSUED,
            PurchaseOrderStatus.PARTIALLY_RECEIVED,
            PurchaseOrderStatus.CLOSED);
    private static final Set<String> TERMINAL_PR_STATUSES = Set.of("CLOSED", "CANCELLED");

    private final VehicleProgramRepository vehicleProgramRepository;
    private final MaterialMasterRepository materialMasterRepository;
    private final BomNodeRepository bomNodeRepository;
    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    private final PackagingLogisticsProfileRepository packagingLogisticsProfileRepository;
    private final TargetCostRecordRepository targetCostRecordRepository;
    private final ApprovalStepInstanceRepository approvalStepInstanceRepository;
    private final TargetCostVarianceRepository targetCostVarianceRepository;
    private final AppNotificationRepository appNotificationRepository;
    private final SupplierRiskRepository supplierRiskRepository;
    private final SupplierPolicyEvidenceRepository supplierPolicyEvidenceRepository;
    private final SupplierWarrantyClaimRepository supplierWarrantyClaimRepository;
    private final SupplierGuaranteeEventRepository supplierGuaranteeEventRepository;
    private final SupplierPerformanceFeedbackRepository supplierPerformanceFeedbackRepository;
    private final VendorRecommendationRepository vendorRecommendationRepository;
    private final SupplierCapabilityRepository supplierCapabilityRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final RfqEventRepository rfqEventRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final ProcessRoutingRepository processRoutingRepository;
    private final ProcessOperationRepository processOperationRepository;
    private final ToolingRecordRepository toolingRecordRepository;
    private final PermissionAuth permissionAuth;
    private final AccessScopeService accessScopeService;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional(readOnly = true)
    public KpiResponse kpis() {
        String customerId = tenantAccessGuard.requireCustomerId();
        LocalDate today = LocalDate.now();

        return new KpiResponse(
                vehicleProgramRepository.countByCustomerId(customerId),
                materialMasterRepository.countByCustomerId(customerId),
                bomNodeRepository.countByCustomerId(customerId),
                shouldCostBuildUpRepository.countByCustomerId(customerId),
                packagingLogisticsProfileRepository.countByCustomerId(customerId),
                targetCostRecordRepository.countByCustomerIdAndStatus(customerId, TargetCostStatus.ACTIVE),
                pendingApprovalStepsForCurrentUser(),
                targetCostVarianceRepository.countByCustomerIdAndExceedsThresholdTrue(customerId),
                unreadNotificationCount(),
                supplierRiskRepository.countByCustomerIdAndStatus(customerId, RiskStatus.OPEN),
                supplierPolicyEvidenceRepository.countByCustomerIdAndEvidenceStatus(customerId, EvidenceStatus.PENDING),
                supplierWarrantyClaimRepository.countByCustomerIdAndClaimStatus(customerId, WarrantyClaimStatus.OPEN),
                supplierGuaranteeEventRepository.countByCustomerIdAndResolvedFlagFalse(customerId),
                supplierPerformanceFeedbackRepository.countByCustomerIdAndEvidenceStatus(customerId, EvidenceStatus.PENDING),
                vendorRecommendationRepository.countByCustomerIdAndStatus(customerId, RecommendationStatus.DRAFT),
                vendorRecommendationRepository.countByCustomerIdAndStatus(customerId, RecommendationStatus.SUBMITTED),
                supplierCapabilityRepository.countByCustomerIdAndStatusAndValidToLessThanEqual(
                        customerId, CapabilityStatus.ACTIVE, today),
                supportOpen(customerId),
                supportInProgress(customerId),
                supportWaiting(customerId),
                supportResolved(customerId),
                gatedLong("RFQ_VIEW", () -> rfqEventRepository.countByCustomerIdAndStatus(customerId, RfqStatus.OPEN)),
                gatedLong("RFQ_VIEW", () -> rfqEventRepository.countByCustomerIdAndStatusAndDueDateBetween(
                        customerId, RfqStatus.OPEN, today, today.plusDays(RFQ_CLOSING_SOON_DAYS))),
                gatedLong("RFQ_VIEW", () -> rfqEventRepository.countOpenWithNoQuotes(customerId, RfqStatus.OPEN)),
                gatedLong("RFQ_VIEW", () -> purchaseOrderRepository.countByCustomerIdAndStatusIn(customerId, OPEN_PO_STATUSES)),
                gatedLong("RFQ_VIEW", () -> purchaseOrderRepository.countByCustomerIdAndStatusInAndRequiredDeliveryDateBefore(
                        customerId, IN_FLIGHT_PO_STATUSES, today)),
                gatedLong("RFQ_VIEW", () -> purchaseRequestRepository.countByCustomerIdAndStatusNotIn(
                        customerId, TERMINAL_PR_STATUSES)),
                buyerOpenOrders(customerId),
                gatedDecimal("RFQ_VIEW", () -> nz(purchaseOrderLineRepository.sumLineValueByCustomerIdAndStatusIn(
                        customerId, SPEND_PO_STATUSES))),
                gatedLong("COSTING_VIEW", () -> materialMasterRepository.countIncompleteByCustomerId(customerId)),
                gatedLong("COSTING_VIEW", () -> processRoutingRepository.countByCustomerIdAndStatus(
                        customerId, RoutingStatus.DRAFT)),
                gatedLong("COSTING_VIEW", () -> processOperationRepository.countMissingMachineOrLaborByCustomerId(customerId)),
                gatedLong("COSTING_VIEW", () -> processOperationRepository.countNonPositiveCycleTimeByCustomerId(customerId)),
                gatedLong("COSTING_VIEW", () -> toolingRecordRepository.countByCustomerIdAndAssetStatus(
                        customerId, ToolingAssetStatus.ACTIVE)),
                gatedDecimal("COSTING_VIEW", () -> nz(toolingRecordRepository.sumToolCostByCustomerIdAndAssetStatus(
                        customerId, ToolingAssetStatus.ACTIVE))));
    }

    private long buyerOpenOrders(String customerId) {
        if (!permissionAuth.has("RFQ_VIEW")) {
            return 0L;
        }
        var user = accessScopeService.currentUserOrNull();
        if (user == null || user.getUserId() == null || user.getUserId().isBlank()) {
            return 0L;
        }
        return purchaseOrderRepository.countByCustomerIdAndBuyerUserIdAndStatusIn(
                customerId, user.getUserId(), OPEN_PO_STATUSES);
    }

    private long gatedLong(String permission, java.util.function.LongSupplier supplier) {
        if (!permissionAuth.has(permission)) {
            return 0L;
        }
        return supplier.getAsLong();
    }

    private BigDecimal gatedDecimal(String permission, java.util.function.Supplier<BigDecimal> supplier) {
        if (!permissionAuth.has(permission)) {
            return BigDecimal.ZERO;
        }
        return nz(supplier.get());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private long supportOpen(String customerId) {
        if (!permissionAuth.has("SUPPORT_TICKET_READ")) {
            return 0L;
        }
        return supportTicketRepository.countByCustomerIdAndStatus(customerId, SupportTicketStatus.OPEN);
    }

    private long supportInProgress(String customerId) {
        if (!permissionAuth.has("SUPPORT_TICKET_READ")) {
            return 0L;
        }
        return supportTicketRepository.countByCustomerIdAndStatus(customerId, SupportTicketStatus.IN_PROGRESS);
    }

    private long supportWaiting(String customerId) {
        if (!permissionAuth.has("SUPPORT_TICKET_READ")) {
            return 0L;
        }
        return supportTicketRepository.countByCustomerIdAndStatus(customerId, SupportTicketStatus.WAITING_FOR_CUSTOMER);
    }

    private long supportResolved(String customerId) {
        if (!permissionAuth.has("SUPPORT_TICKET_READ")) {
            return 0L;
        }
        return supportTicketRepository.countByCustomerIdAndStatus(customerId, SupportTicketStatus.RESOLVED);
    }

    private long pendingApprovalStepsForCurrentUser() {
        var user = accessScopeService.currentUserOrNull();
        if (user == null || user.getUserId() == null) {
            return 0L;
        }
        return approvalStepInstanceRepository
                .findByApproverUserIdAndDecision(user.getUserId(), DecisionStatus.PENDING)
                .size();
    }

    private long unreadNotificationCount() {
        var user = accessScopeService.currentUserOrNull();
        if (user == null || user.getUserId() == null || user.getUserId().isBlank()) {
            return 0L;
        }
        return appNotificationRepository.countByRecipientUserIdAndReadFlag(user.getUserId(), false);
    }
}
