package com.automotive.costing.purchase.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.purchase.dto.PurchaseDtos.DecideRequest;
import com.automotive.costing.purchase.dto.PurchaseDtos.ReleaseRequest;
import com.automotive.costing.purchase.entity.PurchaseRequest;
import com.automotive.costing.purchase.entity.PurchaseRequestLine;
import com.automotive.costing.purchase.repository.PurchaseRequestLineRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PR decision / release must notify the PR creator via NotificationService
 * (in-app always; email when SMTP is enabled).
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PurchaseDecisionNotificationTest {

    @Mock PurchaseRequestRepository requestRepository;
    @Mock private com.automotive.costing.common.outbox.DomainOutboxService domainOutboxService;
    @Mock PurchaseRequestLineRepository lineRepository;
    @Mock AccessScopeService accessScopeService;
    @Mock NotificationService notificationService;
    @Mock SupplierQualificationCurrentRepository qualificationCurrentRepository;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks PurchaseService purchaseService;

    @BeforeEach
    void auth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("approver-1", "n/a", List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private PurchaseRequest submittedPr() {
        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId("pr-1");
        pr.setPrNumber("PR-100");
        pr.setProgramId("prog-1");
        pr.setStatus(PurchaseRequestStatus.SUBMITTED);
        pr.setCreatedBy("creator-user");
        return pr;
    }

    @Test
    void decideApprove_notifiesCreatorWithApprovedType() {
        PurchaseRequest pr = submittedPr();
        when(requestRepository.findByCustomerIdAndPurchaseRequestId("cust-test-1", "pr-1")).thenReturn(Optional.of(pr));
        when(lineRepository.findByPurchaseRequestIdOrderByLineNoAsc("pr-1")).thenReturn(List.of());
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(accessScopeService).assertAccessForCurrentUser(eq(ScopeType.PROGRAM), eq("prog-1"));

        purchaseService.decide("pr-1", new DecideRequest("APPROVE", "Commercially acceptable"));

        ArgumentCaptor<String> userCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<NotificationType> typeCap = ArgumentCaptor.forClass(NotificationType.class);
        verify(notificationService).publish(
                userCap.capture(),
                typeCap.capture(),
                contains("approved"),
                contains("Commercially acceptable"),
                eq(PurchaseService.ENTITY_TYPE),
                eq("pr-1"),
                eq(NotificationSeverity.INFO));
        assertEquals("creator-user", userCap.getValue());
        assertEquals(NotificationType.PURCHASE_REQUEST_APPROVED, typeCap.getValue());
        assertEquals(PurchaseRequestStatus.APPROVED, pr.getStatus());
    }

    @Test
    void decideReject_notifiesCreatorWithRejectedType() {
        PurchaseRequest pr = submittedPr();
        when(requestRepository.findByCustomerIdAndPurchaseRequestId("cust-test-1", "pr-1")).thenReturn(Optional.of(pr));
        when(lineRepository.findByPurchaseRequestIdOrderByLineNoAsc("pr-1")).thenReturn(List.of());
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(accessScopeService).assertAccessForCurrentUser(eq(ScopeType.PROGRAM), eq("prog-1"));

        purchaseService.decide("pr-1", new DecideRequest("REJECT", "Price too high"));

        ArgumentCaptor<NotificationType> typeCap = ArgumentCaptor.forClass(NotificationType.class);
        verify(notificationService).publish(
                eq("creator-user"),
                typeCap.capture(),
                contains("rejected"),
                contains("Price too high"),
                eq(PurchaseService.ENTITY_TYPE),
                eq("pr-1"),
                eq(NotificationSeverity.WARNING));
        assertEquals(NotificationType.PURCHASE_REQUEST_REJECTED, typeCap.getValue());
        assertEquals(PurchaseRequestStatus.REJECTED, pr.getStatus());
    }

    @Test
    void release_notifiesCreatorWithReleasedType() {
        PurchaseRequest pr = submittedPr();
        pr.setStatus(PurchaseRequestStatus.APPROVED);
        when(requestRepository.findByCustomerIdAndPurchaseRequestId("cust-test-1", "pr-1")).thenReturn(Optional.of(pr));
        PurchaseRequestLine line = new PurchaseRequestLine();
        line.setLineNo(1);
        line.setDataStatus("AVAILABLE");
        line.setSupplierId("sup-1");
        when(lineRepository.findByPurchaseRequestIdOrderByLineNoAsc("pr-1")).thenReturn(List.of(line));
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("sup-1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.APPROVED);
        when(qualificationCurrentRepository.findById("sup-1")).thenReturn(Optional.of(cur));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(accessScopeService).assertAccessForCurrentUser(eq(ScopeType.PROGRAM), eq("prog-1"));

        purchaseService.release("pr-1", new ReleaseRequest(null));

        verify(notificationService).publish(
                eq("creator-user"),
                eq(NotificationType.PURCHASE_REQUEST_RELEASED),
                contains("released"),
                contains("does not post to SAP MM"),
                eq(PurchaseService.ENTITY_TYPE),
                eq("pr-1"),
                eq(NotificationSeverity.INFO));
        assertEquals(PurchaseRequestStatus.RELEASED, pr.getStatus());
    }

    @Test
    void decide_skipsNotificationWhenNoCreatedBy() {
        PurchaseRequest pr = submittedPr();
        pr.setCreatedBy(null);
        when(requestRepository.findByCustomerIdAndPurchaseRequestId("cust-test-1", "pr-1")).thenReturn(Optional.of(pr));
        when(lineRepository.findByPurchaseRequestIdOrderByLineNoAsc("pr-1")).thenReturn(List.of());
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(accessScopeService).assertAccessForCurrentUser(eq(ScopeType.PROGRAM), eq("prog-1"));

        purchaseService.decide("pr-1", new DecideRequest("APPROVE", "ok"));

        verify(notificationService, never()).publish(
                anyString(), any(), anyString(), anyString(), anyString(), anyString(), any());
        assertEquals(PurchaseRequestStatus.APPROVED, pr.getStatus());
    }
}
