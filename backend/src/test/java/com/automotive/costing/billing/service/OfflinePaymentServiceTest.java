package com.automotive.costing.billing.service;

import com.automotive.costing.billing.entity.*;
import com.automotive.costing.billing.gateway.PaymentGatewayRegistry;
import com.automotive.costing.billing.repository.*;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.repository.PlatformCustomerRepository;
import com.automotive.costing.platform.access.service.CommercialLicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfflinePaymentServiceTest {

    @Mock BillingPlanRepository planRepository;
    @Mock BillingPlanModuleRepository planModuleRepository;
    @Mock CustomerSubscriptionRepository subscriptionRepository;
    @Mock BillingInvoiceRepository invoiceRepository;
    @Mock BillingPaymentRepository paymentRepository;
    @Mock BillingWebhookEventRepository webhookEventRepository;
    @Mock PlatformCustomerRepository customerRepository;
    @Mock PaymentGatewayRegistry gatewayRegistry;
    @Mock SubscriptionEntitlementService entitlementService;
    @Mock CommercialLicenseService commercialLicenseService;

    @InjectMocks BillingSubscriptionService service;

    private CustomerSubscription sub;

    @BeforeEach
    void setUp() {
        sub = new CustomerSubscription();
        sub.setSubscriptionId("sub-1");
        sub.setCustomerId("cust-a");
        sub.setPlanId("plan-1");
        sub.setStatus(BillingSubscriptionService.ST_PENDING);
        sub.setAmount(new BigDecimal("5000.0000"));
        sub.setCurrencyCode("INR");
        sub.setBillingCycle("MONTHLY");
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusMonths(1));
    }

    @Test
    void offlineSubmit_usesServerAmount_neverClient() {
        when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingPayment pay = service.submitOfflinePayment(
                "cust-a", "sub-1", "BANK_TRANSFER", "UTR-999", "paid via NEFT", "idem-1");

        assertEquals(new BigDecimal("5000.0000"), pay.getAmount());
        assertEquals("OFFLINE", pay.getGatewayCode());
        assertEquals("BANK_TRANSFER", pay.getPaymentMethod());
        assertEquals(BillingSubscriptionService.PAY_UNDER_VERIFICATION, pay.getStatus());
        assertEquals("idem-1", pay.getIdempotencyKey());
    }

    @Test
    void offlineSubmit_wrongCustomer_denied() {
        when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub));
        assertThrows(BusinessRuleViolationException.class,
                () -> service.submitOfflinePayment("cust-b", "sub-1", "NEFT", "x", null, null));
    }

    @Test
    void offlineSubmit_invalidMethod_denied() {
        assertThrows(BusinessRuleViolationException.class,
                () -> service.submitOfflinePayment("cust-a", "sub-1", "CRYPTO", "x", null, null));
    }

    @Test
    void offlineSubmit_idempotent() {
        BillingPayment existing = new BillingPayment();
        existing.setPaymentId("pay-existing");
        existing.setIdempotencyKey("idem-dup");
        when(paymentRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existing));
        BillingPayment result = service.submitOfflinePayment(
                "cust-a", "sub-1", "CHEQUE", "CHQ-1", null, "idem-dup");
        assertEquals("pay-existing", result.getPaymentId());
        verify(subscriptionRepository, never()).findById(any());
    }

    @Test
    void verifyOffline_approve_activates() {
        BillingPayment pay = new BillingPayment();
        pay.setPaymentId("pay-1");
        pay.setInvoiceId("inv-1");
        pay.setSubscriptionId("sub-1");
        pay.setCustomerId("cust-a");
        pay.setGatewayCode("OFFLINE");
        pay.setStatus(BillingSubscriptionService.PAY_UNDER_VERIFICATION);
        pay.setAmount(new BigDecimal("5000.0000"));
        pay.setCurrencyCode("INR");

        BillingInvoice inv = new BillingInvoice();
        inv.setInvoiceId("inv-1");
        inv.setPaymentStatus(BillingSubscriptionService.PAY_PENDING);

        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(pay));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(inv));
        when(subscriptionRepository.findById("sub-1")).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.verifyOfflinePayment("pay-1", true, "platform-user", "UTR verified");

        assertEquals(BillingSubscriptionService.PAY_PAID, pay.getStatus());
        assertEquals(BillingSubscriptionService.ST_ACTIVE, sub.getStatus());
        verify(entitlementService).activatePlanModules(eq("cust-a"), eq("plan-1"), any(), any());
        verify(commercialLicenseService).setStatus(eq("cust-a"), eq("ACTIVE"), isNull(), eq("payment-confirmed"));
    }

    @Test
    void verifyOffline_reject_doesNotActivate() {
        BillingPayment pay = new BillingPayment();
        pay.setPaymentId("pay-1");
        pay.setInvoiceId("inv-1");
        pay.setGatewayCode("OFFLINE");
        pay.setStatus(BillingSubscriptionService.PAY_UNDER_VERIFICATION);
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(pay));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(new BillingInvoice()));

        service.verifyOfflinePayment("pay-1", false, "platform-user", "bad ref");
        assertEquals(BillingSubscriptionService.PAY_REJECTED, pay.getStatus());
        verify(entitlementService, never()).activatePlanModules(any(), any(), any(), any());
    }

    @Test
    void verifyOnlineGatewayAsOffline_denied() {
        BillingPayment pay = new BillingPayment();
        pay.setPaymentId("pay-1");
        pay.setGatewayCode("STUB");
        pay.setStatus(BillingSubscriptionService.PAY_UNDER_VERIFICATION);
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(pay));
        assertThrows(BusinessRuleViolationException.class,
                () -> service.verifyOfflinePayment("pay-1", true, "u", null));
    }
}
