package com.automotive.costing.dashboard;

import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.dashboard.service.DashboardService;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.notification.repository.AppNotificationRepository;
import com.automotive.costing.packaging.repository.PackagingLogisticsProfileRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.process.entity.RoutingStatus;
import com.automotive.costing.process.repository.ProcessOperationRepository;
import com.automotive.costing.process.repository.ProcessRoutingRepository;
import com.automotive.costing.purchase.repository.PurchaseOrderLineRepository;
import com.automotive.costing.purchase.repository.PurchaseOrderRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import com.automotive.costing.rfq.entity.RfqStatus;
import com.automotive.costing.rfq.repository.RfqEventRepository;
import com.automotive.costing.security.PermissionAuth;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.support.entity.SupportTicketStatus;
import com.automotive.costing.support.repository.SupportTicketRepository;
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
import com.automotive.costing.workflow.repository.ApprovalStepInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceVendorKpiTest {

    @Mock private VehicleProgramRepository vehicleProgramRepository;
    @Mock private MaterialMasterRepository materialMasterRepository;
    @Mock private BomNodeRepository bomNodeRepository;
    @Mock private ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    @Mock private PackagingLogisticsProfileRepository packagingLogisticsProfileRepository;
    @Mock private TargetCostRecordRepository targetCostRecordRepository;
    @Mock private TargetCostVarianceRepository targetCostVarianceRepository;
    @Mock private ApprovalStepInstanceRepository approvalStepInstanceRepository;
    @Mock private AppNotificationRepository appNotificationRepository;
    @Mock private SupplierRiskRepository supplierRiskRepository;
    @Mock private SupplierPolicyEvidenceRepository supplierPolicyEvidenceRepository;
    @Mock private SupplierWarrantyClaimRepository supplierWarrantyClaimRepository;
    @Mock private SupplierGuaranteeEventRepository supplierGuaranteeEventRepository;
    @Mock private SupplierPerformanceFeedbackRepository supplierPerformanceFeedbackRepository;
    @Mock private VendorRecommendationRepository vendorRecommendationRepository;
    @Mock private SupplierCapabilityRepository supplierCapabilityRepository;
    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private RfqEventRepository rfqEventRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderLineRepository purchaseOrderLineRepository;
    @Mock private PurchaseRequestRepository purchaseRequestRepository;
    @Mock private ProcessRoutingRepository processRoutingRepository;
    @Mock private ProcessOperationRepository processOperationRepository;
    @Mock private ToolingRecordRepository toolingRecordRepository;
    @Mock private PermissionAuth permissionAuth;
    @Mock private AccessScopeService accessScopeService;
    @Mock private TenantAccessGuard tenantAccessGuard;

    @InjectMocks private DashboardService service;

    @BeforeEach
    void stubTenantAccessGuard() {
        when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
        when(accessScopeService.currentUserOrNull()).thenReturn(null);
        when(permissionAuth.has("SUPPORT_TICKET_READ")).thenReturn(true);
        when(permissionAuth.has("RFQ_VIEW")).thenReturn(false);
        when(permissionAuth.has("COSTING_VIEW")).thenReturn(false);
        stubCoreCounts();
    }

    private void stubCoreCounts() {
        when(vehicleProgramRepository.countByCustomerId("cust-test-1")).thenReturn(1L);
        when(materialMasterRepository.countByCustomerId("cust-test-1")).thenReturn(2L);
        when(bomNodeRepository.countByCustomerId("cust-test-1")).thenReturn(3L);
        when(shouldCostBuildUpRepository.countByCustomerId("cust-test-1")).thenReturn(4L);
        when(packagingLogisticsProfileRepository.countByCustomerId("cust-test-1")).thenReturn(5L);
        when(targetCostRecordRepository.countByCustomerIdAndStatus("cust-test-1", TargetCostStatus.ACTIVE)).thenReturn(6L);
        when(targetCostVarianceRepository.countByCustomerIdAndExceedsThresholdTrue("cust-test-1")).thenReturn(8L);
        when(supplierRiskRepository.countByCustomerIdAndStatus("cust-test-1", RiskStatus.OPEN)).thenReturn(10L);
        when(supplierPolicyEvidenceRepository.countByCustomerIdAndEvidenceStatus("cust-test-1", EvidenceStatus.PENDING)).thenReturn(11L);
        when(supplierWarrantyClaimRepository.countByCustomerIdAndClaimStatus("cust-test-1", WarrantyClaimStatus.OPEN)).thenReturn(12L);
        when(supplierGuaranteeEventRepository.countByCustomerIdAndResolvedFlagFalse("cust-test-1")).thenReturn(13L);
        when(supplierPerformanceFeedbackRepository.countByCustomerIdAndEvidenceStatus("cust-test-1", EvidenceStatus.PENDING)).thenReturn(14L);
        when(vendorRecommendationRepository.countByCustomerIdAndStatus("cust-test-1", RecommendationStatus.DRAFT)).thenReturn(15L);
        when(vendorRecommendationRepository.countByCustomerIdAndStatus("cust-test-1", RecommendationStatus.SUBMITTED)).thenReturn(16L);
        when(supplierCapabilityRepository.countByCustomerIdAndStatusAndValidToLessThanEqual(
                eq("cust-test-1"), eq(CapabilityStatus.ACTIVE), any())).thenReturn(17L);
        when(supportTicketRepository.countByCustomerIdAndStatus("cust-test-1", SupportTicketStatus.OPEN)).thenReturn(3L);
        when(supportTicketRepository.countByCustomerIdAndStatus("cust-test-1", SupportTicketStatus.IN_PROGRESS)).thenReturn(2L);
        when(supportTicketRepository.countByCustomerIdAndStatus("cust-test-1", SupportTicketStatus.WAITING_FOR_CUSTOMER)).thenReturn(1L);
        when(supportTicketRepository.countByCustomerIdAndStatus("cust-test-1", SupportTicketStatus.RESOLVED)).thenReturn(5L);
    }

    @Test
    void kpis_includesVendorSelectionAndSupportCounts() {
        var k = service.kpis();
        assertThat(k.openSupplierRisks()).isEqualTo(10L);
        assertThat(k.pendingPolicyEvidence()).isEqualTo(11L);
        assertThat(k.openWarrantyClaims()).isEqualTo(12L);
        assertThat(k.unresolvedGuaranteeEvents()).isEqualTo(13L);
        assertThat(k.pendingPerformanceFeedback()).isEqualTo(14L);
        assertThat(k.draftRecommendations()).isEqualTo(15L);
        assertThat(k.submittedRecommendations()).isEqualTo(16L);
        assertThat(k.expiringCapabilities()).isEqualTo(17L);
        assertThat(k.activePrograms()).isEqualTo(1L);
        assertThat(k.unreadNotifications()).isEqualTo(0L);
        assertThat(k.openSupportTickets()).isEqualTo(3L);
        assertThat(k.inProgressSupportTickets()).isEqualTo(2L);
        assertThat(k.waitingCustomerSupportTickets()).isEqualTo(1L);
        assertThat(k.resolvedSupportTickets()).isEqualTo(5L);
        assertThat(k.openRfqs()).isZero();
        assertThat(k.purchasingSpend()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void kpis_supportCountsZeroWhenNoPermission() {
        when(permissionAuth.has("SUPPORT_TICKET_READ")).thenReturn(false);
        var k = service.kpis();
        assertThat(k.openSupportTickets()).isEqualTo(0L);
        assertThat(k.inProgressSupportTickets()).isEqualTo(0L);
        assertThat(k.waitingCustomerSupportTickets()).isEqualTo(0L);
        assertThat(k.resolvedSupportTickets()).isEqualTo(0L);
    }

    @Test
    void kpis_rfqAndPoAggregatesWhenRfqViewGranted() {
        when(permissionAuth.has("RFQ_VIEW")).thenReturn(true);
        when(rfqEventRepository.countByCustomerIdAndStatus("cust-test-1", RfqStatus.OPEN)).thenReturn(9L);
        when(rfqEventRepository.countByCustomerIdAndStatusAndDueDateBetween(
                eq("cust-test-1"), eq(RfqStatus.OPEN), any(LocalDate.class), any(LocalDate.class))).thenReturn(2L);
        when(rfqEventRepository.countOpenWithNoQuotes("cust-test-1", RfqStatus.OPEN)).thenReturn(4L);
        when(purchaseOrderRepository.countByCustomerIdAndStatusIn(eq("cust-test-1"), any())).thenReturn(7L);
        when(purchaseOrderRepository.countByCustomerIdAndStatusInAndRequiredDeliveryDateBefore(
                eq("cust-test-1"), any(), any(LocalDate.class))).thenReturn(1L);
        when(purchaseRequestRepository.countByCustomerIdAndStatusNotIn(eq("cust-test-1"), any())).thenReturn(3L);
        when(purchaseOrderLineRepository.sumLineValueByCustomerIdAndStatusIn(eq("cust-test-1"), any()))
                .thenReturn(new BigDecimal("1250.500000"));

        var k = service.kpis();
        assertThat(k.openRfqs()).isEqualTo(9L);
        assertThat(k.rfqsClosingSoon()).isEqualTo(2L);
        assertThat(k.awaitingQuotations()).isEqualTo(4L);
        assertThat(k.openPurchaseOrders()).isEqualTo(7L);
        assertThat(k.overduePurchaseOrders()).isEqualTo(1L);
        assertThat(k.openPurchaseRequests()).isEqualTo(3L);
        assertThat(k.purchasingSpend()).isEqualByComparingTo(new BigDecimal("1250.500000"));
        verify(rfqEventRepository).countByCustomerIdAndStatus("cust-test-1", RfqStatus.OPEN);
    }

    @Test
    void kpis_engineeringAggregatesWhenCostingViewGranted() {
        when(permissionAuth.has("COSTING_VIEW")).thenReturn(true);
        when(materialMasterRepository.countIncompleteByCustomerId("cust-test-1")).thenReturn(6L);
        when(processRoutingRepository.countByCustomerIdAndStatus("cust-test-1", RoutingStatus.DRAFT)).thenReturn(5L);
        when(processOperationRepository.countMissingMachineOrLaborByCustomerId("cust-test-1")).thenReturn(8L);
        when(processOperationRepository.countNonPositiveCycleTimeByCustomerId("cust-test-1")).thenReturn(2L);
        when(toolingRecordRepository.countByCustomerIdAndAssetStatus("cust-test-1", ToolingAssetStatus.ACTIVE)).thenReturn(11L);
        when(toolingRecordRepository.sumToolCostByCustomerIdAndAssetStatus("cust-test-1", ToolingAssetStatus.ACTIVE))
                .thenReturn(new BigDecimal("88000.000000"));

        var k = service.kpis();
        assertThat(k.incompleteMaterialMasters()).isEqualTo(6L);
        assertThat(k.draftRoutings()).isEqualTo(5L);
        assertThat(k.routingMissingAssignments()).isEqualTo(8L);
        assertThat(k.cycleTimeExceptions()).isEqualTo(2L);
        assertThat(k.activeToolingRecords()).isEqualTo(11L);
        assertThat(k.toolingInvestment()).isEqualByComparingTo(new BigDecimal("88000.000000"));
    }

    @Test
    void kpis_rfqDeniedDoesNotQueryRfqRepos() {
        when(permissionAuth.has("RFQ_VIEW")).thenReturn(false);
        service.kpis();
        verify(rfqEventRepository, never()).countByCustomerIdAndStatus(any(), any());
        verify(purchaseOrderLineRepository, never()).sumLineValueByCustomerIdAndStatusIn(any(), any());
    }

    @Test
    void kpis_usesTenantCustomerId() {
        service.kpis();
        verify(tenantAccessGuard).requireCustomerId();
        verify(vehicleProgramRepository).countByCustomerId("cust-test-1");
        verify(materialMasterRepository).countByCustomerId("cust-test-1");
    }

    @Test
    void kpis_buyerWorkloadUsesCurrentUser() {
        when(permissionAuth.has("RFQ_VIEW")).thenReturn(true);
        User user = new User();
        user.setUserId("buyer-1");
        when(accessScopeService.currentUserOrNull()).thenReturn(user);
        when(purchaseOrderRepository.countByCustomerIdAndBuyerUserIdAndStatusIn(
                eq("cust-test-1"), eq("buyer-1"), any())).thenReturn(4L);
        when(rfqEventRepository.countByCustomerIdAndStatus(any(), any())).thenReturn(0L);
        when(rfqEventRepository.countByCustomerIdAndStatusAndDueDateBetween(any(), any(), any(), any())).thenReturn(0L);
        when(rfqEventRepository.countOpenWithNoQuotes(any(), any())).thenReturn(0L);
        when(purchaseOrderRepository.countByCustomerIdAndStatusIn(any(), any())).thenReturn(0L);
        when(purchaseOrderRepository.countByCustomerIdAndStatusInAndRequiredDeliveryDateBefore(any(), any(), any())).thenReturn(0L);
        when(purchaseRequestRepository.countByCustomerIdAndStatusNotIn(any(), any())).thenReturn(0L);
        when(purchaseOrderLineRepository.sumLineValueByCustomerIdAndStatusIn(any(), any())).thenReturn(BigDecimal.ZERO);

        var k = service.kpis();
        assertThat(k.buyerOpenOrders()).isEqualTo(4L);
    }
}
