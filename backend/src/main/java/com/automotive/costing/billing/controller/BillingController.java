package com.automotive.costing.billing.controller;

import com.automotive.costing.billing.entity.*;
import com.automotive.costing.billing.service.BillingAccessPolicyService;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.automotive.costing.billing.service.BillingSubscriptionService;
import com.automotive.costing.billing.service.BillingSubscriptionService.PaymentInitiation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingSubscriptionService billingService;
    private final BillingAccessPolicyService accessPolicyService;
    private final TenantAccessGuard tenantAccessGuard;

    @GetMapping("/plans")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER','CUSTOMER_ADMIN','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER')")
    public List<BillingPlan> listPlans() {
        return billingService.listActivePlans();
    }

    @GetMapping("/plans/{planId}/modules")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<BillingPlanModule> planModules(@PathVariable String planId) {
        return billingService.planModules(planId);
    }

    @GetMapping("/customers/{customerId}/subscriptions")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<CustomerSubscription> subscriptions(@PathVariable String customerId) {
        return billingService.listByCustomer(customerId);
    }

    @PostMapping("/subscriptions")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public CustomerSubscription createSubscription(@Valid @RequestBody CreateSubscriptionRequest req) {
        return billingService.createSubscription(req.customerId(), req.planId(), req.autoRenewal());
    }

    @PostMapping("/subscriptions/{subscriptionId}/payments")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public PaymentInitiation initiatePayment(
            @PathVariable String subscriptionId,
            @Valid @RequestBody InitiatePaymentRequest req) {
        return billingService.initiatePayment(subscriptionId, req.gatewayCode(), req.returnUrl());
    }

    @GetMapping("/customers/{customerId}/invoices")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<BillingInvoice> invoices(@PathVariable String customerId) {
        return billingService.listInvoices(customerId);
    }

    @GetMapping("/customers/{customerId}/payments")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<BillingPayment> payments(@PathVariable String customerId) {
        return billingService.listPayments(customerId);
    }

    /**
     * Gateway webhook — no JWT. Signature verification is mandatory inside the service.
     */
    @PostMapping("/webhooks/{gatewayCode}")
    public ResponseEntity<Map<String, String>> webhook(
            @PathVariable String gatewayCode,
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload,
            @RequestParam String eventId,
            @RequestParam String eventType) {
        Map<String, String> normalized = headers.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (a, b) -> a));
        String result = billingService.processWebhook(gatewayCode, eventId, eventType, payload, normalized);
        return ResponseEntity.ok(Map.of("result", result));
    }


    // ── Customer Admin: own-tenant read + offline submit (never self-approve) ──

    @GetMapping("/my/subscriptions")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    public List<CustomerSubscription> mySubscriptions() {
        String customerId = tenantAccessGuard.requireCustomerId();
        return billingService.listByCustomer(customerId);
    }

    @GetMapping("/my/invoices")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    public List<BillingInvoice> myInvoices() {
        String customerId = tenantAccessGuard.requireCustomerId();
        return billingService.listInvoices(customerId);
    }

    @GetMapping("/my/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    public List<BillingPayment> myPayments() {
        String customerId = tenantAccessGuard.requireCustomerId();
        return billingService.listPayments(customerId);
    }

    @PostMapping("/my/offline-payments")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    public BillingPayment submitOffline(@Valid @RequestBody OfflinePaymentRequest req) {
        String customerId = tenantAccessGuard.requireCustomerId();
        return billingService.submitOfflinePayment(
                customerId, req.subscriptionId(), req.paymentMethod(),
                req.externalReference(), req.notes(), req.idempotencyKey());
    }

    @GetMapping("/offline-payments/pending")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    public List<BillingPayment> pendingOffline() {
        return billingService.listPendingOffline();
    }

    @PostMapping("/payments/{paymentId}/verify-offline")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    public BillingPayment verifyOffline(
            @PathVariable String paymentId,
            @Valid @RequestBody VerifyOfflineRequest req) {
        String actor = currentUsername();
        return billingService.verifyOfflinePayment(paymentId, req.approve(), actor, req.notes());
    }

    private static String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : "system";
    }



    @PostMapping("/payments/{paymentId}/verify-checkout")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    public BillingPayment verifyCheckout(
            @PathVariable String paymentId,
            @Valid @RequestBody CheckoutVerifyRequest req) {
        return billingService.verifyCheckoutPayment(
                paymentId, req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
    }

    @PostMapping("/payments/{paymentId}/verify")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    public BillingPayment verifyPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody VerifyPaymentRequest req) {
        return billingService.verifyProviderPayment(paymentId, req.providerPaymentId());
    }

    @PostMapping("/jobs/expire-subscriptions")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<Map<String, String>> expireJob() {
        billingService.expireDueSubscriptions();
        return ResponseEntity.ok(Map.of("status", "OK"));
    }


    @PostMapping("/payments/{paymentId}/refunds")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public BillingRefund recordRefund(
            @PathVariable String paymentId,
            @Valid @RequestBody RefundRequest req) {
        return accessPolicyService.recordRefund(paymentId, req.amount(), req.reason(), req.accessPolicy());
    }

    @GetMapping("/refunds")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<BillingRefund> listRefunds() {
        return accessPolicyService.listRefunds();
    }

    @PostMapping("/payments/{paymentId}/chargebacks")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public BillingChargeback recordChargeback(
            @PathVariable String paymentId,
            @Valid @RequestBody ChargebackRequest req) {
        return accessPolicyService.recordChargeback(paymentId, req.amount(), req.gatewayCbRef(), req.accessPolicy());
    }

    @GetMapping("/chargebacks")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<BillingChargeback> listChargebacks() {
        return accessPolicyService.listChargebacks();
    }

    @GetMapping("/reports/revenue-snapshot")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public BillingAccessPolicyService.RevenueSnapshot revenueSnapshot() {
        return accessPolicyService.revenueSnapshot();
    }

    public record CreateSubscriptionRequest(
            @NotBlank String customerId,
            @NotBlank String planId,
            boolean autoRenewal) {}

    public record InitiatePaymentRequest(
            @NotBlank String gatewayCode,
            String returnUrl) {}

    public record RefundRequest(
            @NotNull java.math.BigDecimal amount,
            String reason,
            String accessPolicy) {}

    public record CheckoutVerifyRequest(
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature) {}

    public record VerifyPaymentRequest(
            @NotBlank String providerPaymentId) {}

    public record OfflinePaymentRequest(
            @NotBlank String subscriptionId,
            @NotBlank String paymentMethod,
            String externalReference,
            String notes,
            String idempotencyKey) {}

    public record VerifyOfflineRequest(
            boolean approve,
            String notes) {}

    public record ChargebackRequest(
            @NotNull java.math.BigDecimal amount,
            String gatewayCbRef,
            String accessPolicy) {}
}

