package com.automotive.costing.billing.service;

import com.automotive.costing.billing.entity.BillingPayment;
import com.automotive.costing.billing.entity.BillingRefund;
import com.automotive.costing.billing.entity.CustomerSubscription;
import com.automotive.costing.billing.gateway.PaymentGatewayRegistry;
import com.automotive.costing.billing.repository.BillingChargebackRepository;
import com.automotive.costing.billing.repository.BillingPaymentRepository;
import com.automotive.costing.billing.repository.BillingRefundRepository;
import com.automotive.costing.billing.repository.CustomerSubscriptionRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingAccessPolicyServiceTest {

    @Mock BillingPaymentRepository paymentRepository;
    @Mock PaymentGatewayRegistry gatewayRegistry;
    @Mock BillingRefundRepository refundRepository;
    @Mock BillingChargebackRepository chargebackRepository;
    @Mock CustomerSubscriptionRepository subscriptionRepository;
    @Mock SubscriptionEntitlementService entitlementService;

    BillingAccessPolicyService service;

    @BeforeEach
    void setUp() {
        service = new BillingAccessPolicyService(
                paymentRepository, gatewayRegistry, refundRepository, chargebackRepository,
                subscriptionRepository, entitlementService);
        lenient().when(gatewayRegistry.require(any())).thenThrow(
                new BusinessRuleViolationException("Unknown payment gateway", "BILLING-01"));
    }

    private BillingPayment paidPayment() {
        BillingPayment p = new BillingPayment();
        p.setPaymentId("pay-1");
        p.setSubscriptionId("sub-1");
        p.setCustomerId("cust-1");
        p.setAmount(new BigDecimal("100.0000"));
        p.setCurrencyCode("INR");
        p.setStatus(BillingSubscriptionService.PAY_PAID);
        p.setGatewayCode("STUB");
        return p;
    }

    private CustomerSubscription sub() {
        CustomerSubscription s = new CustomerSubscription();
        s.setSubscriptionId("sub-1");
        s.setCustomerId("cust-1");
        s.setPlanId("plan-1");
        s.setStatus(BillingSubscriptionService.ST_ACTIVE);
        return s;
    }

    @Test
    void refundImmediateRevoke_deactivatesEntitlements() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(paidPayment()));
        when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub()));
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BillingRefund r = service.recordRefund(
                "pay-1", new BigDecimal("50"), "test", BillingAccessPolicyService.POLICY_IMMEDIATE_REVOKE);

        assertEquals(BillingAccessPolicyService.REF_COMPLETED, r.getStatus());
        assertEquals(BillingAccessPolicyService.POLICY_IMMEDIATE_REVOKE, r.getAccessPolicy());
        verify(entitlementService).deactivatePlanModules("cust-1", "plan-1");
        ArgumentCaptor<CustomerSubscription> cap = ArgumentCaptor.forClass(CustomerSubscription.class);
        verify(subscriptionRepository).save(cap.capture());
        assertEquals(BillingSubscriptionService.ST_SUSPENDED, cap.getValue().getStatus());
    }

    @Test
    void refundOnNonPaid_rejected() {
        BillingPayment p = paidPayment();
        p.setStatus(BillingSubscriptionService.PAY_PENDING);
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(p));
        assertThrows(BusinessRuleViolationException.class,
                () -> service.recordRefund("pay-1", new BigDecimal("10"), null,
                        BillingAccessPolicyService.POLICY_KEEP_UNTIL_PERIOD_END));
        verify(entitlementService, never()).deactivatePlanModules(any(), any());
    }

    @Test
    void refundExceedsPayment_rejected() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(paidPayment()));
        assertThrows(BusinessRuleViolationException.class,
                () -> service.recordRefund("pay-1", new BigDecimal("200"), null,
                        BillingAccessPolicyService.POLICY_KEEP_UNTIL_PERIOD_END));
    }

    @Test
    void revenueSnapshot_netsRefunds() {
        BillingPayment p = paidPayment();
        when(paymentRepository.findByStatus(BillingSubscriptionService.PAY_PAID)).thenReturn(List.of(p));
        BillingRefund r = new BillingRefund();
        r.setAmount(new BigDecimal("40.0000"));
        r.setStatus(BillingAccessPolicyService.REF_COMPLETED);
        when(refundRepository.findAll()).thenReturn(List.of(r));
        when(chargebackRepository.findAll()).thenReturn(List.of());

        var snap = service.revenueSnapshot();
        assertEquals(new BigDecimal("100.0000"), snap.paidTotal());
        assertEquals(new BigDecimal("40.0000"), snap.refundTotal());
        assertEquals(new BigDecimal("60.0000"), snap.netTotal());
    }
}
