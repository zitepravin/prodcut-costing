package com.automotive.costing.billing.service;

import com.automotive.costing.billing.entity.*;
import com.automotive.costing.billing.gateway.PaymentGateway;
import com.automotive.costing.billing.gateway.PaymentGatewayRegistry;
import com.automotive.costing.billing.repository.*;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.repository.PlatformCustomerRepository;
import com.automotive.costing.platform.access.service.CommercialLicenseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingSubscriptionService {

    public static final String ST_TRIAL = "TRIAL";
    public static final String ST_PENDING = "PENDING";
    public static final String ST_ACTIVE = "ACTIVE";
    public static final String ST_PAST_DUE = "PAST_DUE";
    public static final String ST_GRACE = "GRACE_PERIOD";
    public static final String ST_EXPIRED = "EXPIRED";
    public static final String ST_CANCELLED = "CANCELLED";
    public static final String ST_SUSPENDED = "SUSPENDED";

    public static final String PAY_INITIATED = "INITIATED";
    public static final String PAY_PENDING = "PENDING";
    public static final String PAY_PAID = "PAID";
    public static final String PAY_FAILED = "FAILED";
    public static final String PAY_SUBMITTED = "PAYMENT_SUBMITTED";
    public static final String PAY_UNDER_VERIFICATION = "UNDER_VERIFICATION";
    public static final String PAY_REJECTED = "REJECTED";
    public static final String PAY_CANCELLED = "CANCELLED";

    public static final Set<String> OFFLINE_METHODS = Set.of(
            "BANK_TRANSFER", "NEFT", "RTGS", "IMPS", "UPI", "CHEQUE",
            "PURCHASE_ORDER", "MANUAL", "OTHER");

    private final BillingPlanRepository planRepository;
    private final BillingPlanModuleRepository planModuleRepository;
    private final CustomerSubscriptionRepository subscriptionRepository;
    private final BillingInvoiceRepository invoiceRepository;
    private final BillingPaymentRepository paymentRepository;
    private final BillingWebhookEventRepository webhookEventRepository;
    private final PlatformCustomerRepository customerRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final SubscriptionEntitlementService entitlementService;
    private final CommercialLicenseService commercialLicenseService;
    private static final Logger log = LoggerFactory.getLogger(BillingSubscriptionService.class);

    public List<BillingPlan> listActivePlans() {
        return planRepository.findByStatusOrderByPlanCodeAsc("ACTIVE");
    }

    public List<BillingPlanModule> planModules(String planId) {
        return planModuleRepository.findByPlanId(planId);
    }

    public List<CustomerSubscription> listByCustomer(String customerId) {
        return subscriptionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<BillingInvoice> listInvoices(String customerId) {
        return invoiceRepository.findByCustomerIdOrderByInvoiceDateDesc(customerId);
    }

    public List<BillingPayment> listPayments(String customerId) {
        return paymentRepository.findByCustomerIdOrderByInitiatedAtDesc(customerId);
    }

    @Transactional
    public CustomerSubscription createSubscription(String customerId, String planId, boolean autoRenewal) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        BillingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
        if (!"ACTIVE".equals(plan.getStatus())) {
            throw new BusinessRuleViolationException("Plan is not commercially active", "BILLING-02");
        }

        LocalDate start = LocalDate.now();
        CustomerSubscription sub = new CustomerSubscription();
        sub.setSubscriptionId(UUID.randomUUID().toString());
        sub.setCustomerId(customerId);
        sub.setPlanId(planId);
        sub.setStartDate(start);
        sub.setBillingCycle(plan.getBillingFrequency());
        sub.setAmount(plan.getPrice().setScale(4, RoundingMode.HALF_UP));
        sub.setCurrencyCode(plan.getCurrencyCode());
        sub.setAutoRenewal(autoRenewal);
        sub.setEndDate(computeEndDate(start, plan.getBillingFrequency()));
        sub.setRenewalDate(sub.getEndDate());
        if (plan.getTrialDays() > 0) {
            sub.setStatus(ST_TRIAL);
            sub.setTrialEndDate(start.plusDays(plan.getTrialDays()));
            // Trial activates modules until trial end — commercial decision via entitlement dates
            entitlementService.activatePlanModules(customerId, planId, start, sub.getTrialEndDate());
        } else {
            sub.setStatus(ST_PENDING);
        }
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public PaymentInitiation initiatePayment(String subscriptionId, String gatewayCode, String returnUrl) {
        CustomerSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        if (ST_CANCELLED.equals(sub.getStatus()) || ST_EXPIRED.equals(sub.getStatus())) {
            throw new BusinessRuleViolationException("Cannot pay for subscription in status " + sub.getStatus(), "BILLING-03");
        }

        BillingInvoice inv = new BillingInvoice();
        inv.setInvoiceId(UUID.randomUUID().toString());
        inv.setInvoiceNumber("INV-" + System.currentTimeMillis());
        inv.setCustomerId(sub.getCustomerId());
        inv.setSubscriptionId(sub.getSubscriptionId());
        inv.setInvoiceDate(LocalDate.now());
        inv.setDueDate(LocalDate.now().plusDays(7));
        inv.setCurrencyCode(sub.getCurrencyCode());
        inv.setSubtotal(sub.getAmount());
        inv.setTaxAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        inv.setDiscountAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        inv.setTotalAmount(sub.getAmount());
        inv.setPaymentStatus(PAY_PENDING);
        invoiceRepository.save(inv);

        BillingPayment pay = new BillingPayment();
        pay.setPaymentId(UUID.randomUUID().toString());
        pay.setInvoiceId(inv.getInvoiceId());
        pay.setSubscriptionId(sub.getSubscriptionId());
        pay.setCustomerId(sub.getCustomerId());
        pay.setGatewayCode(gatewayCode);
        pay.setAmount(inv.getTotalAmount());
        pay.setCurrencyCode(inv.getCurrencyCode());
        pay.setStatus(PAY_INITIATED);
        pay.setInitiatedAt(Instant.now());
        paymentRepository.save(pay);

        PaymentGateway gateway = gatewayRegistry.require(gatewayCode);
        PaymentGateway.InitiateResult result = gateway.initiatePayment(new PaymentGateway.InitiateRequest(
                pay.getPaymentId(),
                inv.getInvoiceId(),
                pay.getAmount(),
                pay.getCurrencyCode(),
                pay.getCustomerId(),
                returnUrl,
                Map.of("subscriptionId", sub.getSubscriptionId())));
        pay.setGatewayPaymentRef(result.gatewayPaymentRef());
        pay.setStatus(PAY_PENDING);
        paymentRepository.save(pay);

        String keyId = result.publicKeyId() != null ? result.publicKeyId()
                : gateway.publicKeyId().orElse(null);
        long subunits = result.amountSubunits() > 0 ? result.amountSubunits()
                : pay.getAmount().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        return new PaymentInitiation(
                pay.getPaymentId(),
                inv.getInvoiceId(),
                result.gatewayPaymentRef(),
                result.redirectUrl(),
                keyId,
                subunits,
                pay.getCurrencyCode(),
                pay.getAmount());
    }

    /**
     * Webhook processing: signature → idempotency → mark PAID → activate subscription + CAM entitlements.
     * Frontend payment success is never trusted.
     */
    @Transactional
    public String processWebhook(String gatewayCode, String gatewayEventId, String eventType,
                                 String payload, Map<String, String> headers) {
        PaymentGateway gateway = gatewayRegistry.require(gatewayCode);
        boolean sigOk = gateway.verifyWebhookSignature(payload, headers);

        var existing = webhookEventRepository.findByGatewayCodeAndGatewayEventId(gatewayCode, gatewayEventId);
        if (existing.isPresent() && existing.get().isProcessed()) {
            return "DUPLICATE_IGNORED";
        }

        BillingWebhookEvent event = existing.orElseGet(BillingWebhookEvent::new);
        if (event.getWebhookEventId() == null) {
            event.setWebhookEventId(UUID.randomUUID().toString());
            event.setGatewayCode(gatewayCode);
            event.setGatewayEventId(gatewayEventId);
            event.setReceivedAt(Instant.now());
        }
        event.setEventType(eventType);
        event.setPayloadHash(sha256(payload));
        event.setSignatureValid(sigOk);
        event.setRawPayload(payload);

        if (!sigOk) {
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            event.setProcessingResult("SIGNATURE_INVALID");
            webhookEventRepository.save(event);
            throw new BusinessRuleViolationException("Webhook signature invalid", "BILLING-04");
        }

        if ("payment.paid".equalsIgnoreCase(eventType) || "PAYMENT_PAID".equalsIgnoreCase(eventType)
                || "payment.captured".equalsIgnoreCase(eventType) || "payment.authorized".equalsIgnoreCase(eventType)) {
            String paymentId = extractJsonString(payload, "paymentId");
            if (paymentId == null || paymentId.isBlank()) {
                paymentId = extractNestedNotesPaymentId(payload);
            }
            if (paymentId == null || paymentId.isBlank()) {
                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                event.setProcessingResult("MISSING_PAYMENT_ID");
                webhookEventRepository.save(event);
                throw new BusinessRuleViolationException("Webhook missing paymentId", "BILLING-05");
            }
            markPaidAndActivate(paymentId);
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            event.setProcessingResult("PAID_ACTIVATED");
            webhookEventRepository.save(event);
            return "PAID_ACTIVATED";
        }

        if ("payment.failed".equalsIgnoreCase(eventType) || "PAYMENT_FAILED".equalsIgnoreCase(eventType)) {
            String paymentId = extractJsonString(payload, "paymentId");
            if (paymentId != null) {
                paymentRepository.findById(paymentId).ifPresent(p -> {
                    p.setStatus(PAY_FAILED);
                    p.setFailureReason(extractJsonString(payload, "reason"));
                    paymentRepository.save(p);
                });
            }
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            event.setProcessingResult("FAILED_RECORDED");
            webhookEventRepository.save(event);
            return "FAILED_RECORDED";
        }

        event.setProcessed(true);
        event.setProcessedAt(Instant.now());
        event.setProcessingResult("EVENT_IGNORED");
        webhookEventRepository.save(event);
        return "EVENT_IGNORED";
    }

    @Transactional
    public void markPaidAndActivate(String paymentId) {
        BillingPayment pay = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        if (PAY_PAID.equals(pay.getStatus())) {
            return; // idempotent
        }
        pay.setStatus(PAY_PAID);
        pay.setPaidAt(Instant.now());
        paymentRepository.save(pay);

        invoiceRepository.findById(pay.getInvoiceId()).ifPresent(inv -> {
            inv.setPaymentStatus(PAY_PAID);
            inv.setPaymentReference(pay.getGatewayPaymentRef());
            invoiceRepository.save(inv);
        });

        CustomerSubscription sub = subscriptionRepository.findById(pay.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        sub.setStatus(ST_ACTIVE);
        if (sub.getEndDate() == null) {
            sub.setEndDate(computeEndDate(LocalDate.now(), sub.getBillingCycle()));
        }
        subscriptionRepository.save(sub);

        entitlementService.activatePlanModules(
                sub.getCustomerId(), sub.getPlanId(), sub.getStartDate(), sub.getEndDate());
        syncCommercialLicenseActive(sub.getCustomerId());
    }

    private void syncCommercialLicenseActive(String customerId) {
        try {
            commercialLicenseService.setStatus(customerId, "ACTIVE", null, "payment-confirmed");
        } catch (Exception e) {
            log.warn("Commercial license sync after payment for customer={}: {}", customerId, e.toString());
        }
    }

    /**
     * Offline payment submission. Amount from subscription only (never client amount).
     * Customer Admin may submit; cannot approve.
     */
    @Transactional
    public BillingPayment submitOfflinePayment(String customerId, String subscriptionId,
                                               String paymentMethod, String externalReference,
                                               String notes, String idempotencyKey) {
        if (paymentMethod == null || !OFFLINE_METHODS.contains(paymentMethod.toUpperCase())) {
            throw new BusinessRuleViolationException(
                    "Unsupported offline payment method: " + paymentMethod, "BILLING-OFF-01");
        }
        paymentMethod = paymentMethod.toUpperCase();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        CustomerSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        if (!customerId.equals(sub.getCustomerId())) {
            throw new BusinessRuleViolationException("Subscription does not belong to customer", "BILLING-OFF-02");
        }
        if (ST_CANCELLED.equals(sub.getStatus()) || ST_EXPIRED.equals(sub.getStatus())) {
            throw new BusinessRuleViolationException("Cannot pay subscription in status " + sub.getStatus(), "BILLING-03");
        }

        BillingInvoice inv = new BillingInvoice();
        inv.setInvoiceId(UUID.randomUUID().toString());
        inv.setInvoiceNumber("INV-OFF-" + System.currentTimeMillis());
        inv.setCustomerId(customerId);
        inv.setSubscriptionId(subscriptionId);
        inv.setInvoiceDate(LocalDate.now());
        inv.setDueDate(LocalDate.now().plusDays(14));
        inv.setCurrencyCode(sub.getCurrencyCode());
        inv.setSubtotal(sub.getAmount());
        inv.setTaxAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        inv.setDiscountAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        inv.setTotalAmount(sub.getAmount());
        inv.setPaymentStatus(PAY_PENDING);
        invoiceRepository.save(inv);

        BillingPayment pay = new BillingPayment();
        pay.setPaymentId(UUID.randomUUID().toString());
        pay.setInvoiceId(inv.getInvoiceId());
        pay.setSubscriptionId(subscriptionId);
        pay.setCustomerId(customerId);
        pay.setGatewayCode("OFFLINE");
        pay.setPaymentMethod(paymentMethod);
        pay.setAmount(sub.getAmount());
        pay.setCurrencyCode(sub.getCurrencyCode());
        pay.setStatus(PAY_UNDER_VERIFICATION);
        pay.setInitiatedAt(Instant.now());
        pay.setExternalReference(externalReference);
        pay.setVerificationNotes(notes);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            pay.setIdempotencyKey(idempotencyKey);
        }
        return paymentRepository.save(pay);
    }

    @Transactional
    public BillingPayment verifyOfflinePayment(String paymentId, boolean approve,
                                               String platformUserId, String notes) {
        BillingPayment pay = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        if (!PAY_UNDER_VERIFICATION.equals(pay.getStatus()) && !PAY_SUBMITTED.equals(pay.getStatus())) {
            throw new BusinessRuleViolationException(
                    "Payment not under verification: " + pay.getStatus(), "BILLING-OFF-03");
        }
        if (!"OFFLINE".equals(pay.getGatewayCode())) {
            throw new BusinessRuleViolationException("Not an offline payment", "BILLING-OFF-04");
        }
        pay.setVerifiedBy(platformUserId);
        pay.setVerifiedAt(Instant.now());
        if (notes != null) {
            pay.setVerificationNotes(notes);
        }
        if (approve) {
            paymentRepository.save(pay);
            markPaidAndActivate(paymentId);
            return paymentRepository.findById(paymentId).orElse(pay);
        }
        pay.setStatus(PAY_REJECTED);
        paymentRepository.save(pay);
        invoiceRepository.findById(pay.getInvoiceId()).ifPresent(inv -> {
            inv.setPaymentStatus(PAY_FAILED);
            invoiceRepository.save(inv);
        });
        return pay;
    }

    public List<BillingPayment> listPendingOffline() {
        return paymentRepository.findByStatusOrderByInitiatedAtAsc(PAY_UNDER_VERIFICATION);
    }


    /**
     * Razorpay Standard Checkout signature verification.
     * HMAC-SHA256(order_id + "|" + payment_id, key_secret). Does not trust client amount.
     */
    @Transactional
    public BillingPayment verifyCheckoutPayment(
            String paymentId,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {
        BillingPayment pay = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        if (PAY_PAID.equals(pay.getStatus())) {
            return pay;
        }
        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null
                || razorpayOrderId.isBlank() || razorpayPaymentId.isBlank() || razorpaySignature.isBlank()) {
            throw new BusinessRuleViolationException("Missing Razorpay checkout fields", "BILLING-CHK-01");
        }
        if (pay.getGatewayPaymentRef() != null && !pay.getGatewayPaymentRef().isBlank()
                && !pay.getGatewayPaymentRef().equals(razorpayOrderId)) {
            throw new BusinessRuleViolationException("Order id mismatch", "BILLING-CHK-02");
        }
        PaymentGateway gateway = gatewayRegistry.require(pay.getGatewayCode());
        if (!gateway.verifyCheckoutSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature)) {
            throw new BusinessRuleViolationException("Checkout signature invalid", "BILLING-CHK-03");
        }
        // Optional provider amount check when configured
        var v = gateway.verifyPayment(new PaymentGateway.VerificationRequest(
                pay.getPaymentId(), pay.getGatewayPaymentRef(), razorpayPaymentId,
                pay.getAmount(), pay.getCurrencyCode()));
        if (v.isPresent() && !v.get().success() && !"CONFIG".equals(v.get().status())) {
            pay.setStatus(PAY_FAILED);
            pay.setFailureReason(v.get().failureReason());
            paymentRepository.save(pay);
            throw new BusinessRuleViolationException(
                    "Payment verification failed: " + pay.getFailureReason(), "BILLING-VERIFY-02");
        }
        markPaidAndActivate(paymentId);
        return paymentRepository.findById(paymentId).orElse(pay);
    }

    @Transactional
    public BillingPayment verifyProviderPayment(String paymentId, String providerPaymentId) {
        BillingPayment pay = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        if (PAY_PAID.equals(pay.getStatus())) {
            return pay;
        }
        PaymentGateway gateway = gatewayRegistry.require(pay.getGatewayCode());
        var result = gateway.verifyPayment(new PaymentGateway.VerificationRequest(
                pay.getPaymentId(), pay.getGatewayPaymentRef(), providerPaymentId,
                pay.getAmount(), pay.getCurrencyCode()));
        if (result.isEmpty()) {
            throw new BusinessRuleViolationException("Gateway does not support verification", "BILLING-VERIFY-01");
        }
        var v = result.get();
        if (!v.success()) {
            pay.setStatus(PAY_FAILED);
            pay.setFailureReason(v.failureReason() == null ? "verification failed" : v.failureReason());
            paymentRepository.save(pay);
            throw new BusinessRuleViolationException("Payment verification failed: " + pay.getFailureReason(), "BILLING-VERIFY-02");
        }
        markPaidAndActivate(paymentId);
        return paymentRepository.findById(paymentId).orElse(pay);
    }

    @Transactional
    public void expireDueSubscriptions() {
        LocalDate today = LocalDate.now();
        for (CustomerSubscription sub : subscriptionRepository.findByStatus(ST_ACTIVE)) {
            if (sub.getEndDate() != null && sub.getEndDate().isBefore(today)) {
                BillingPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
                int grace = plan != null ? plan.getGraceDays() : 0;
                if (grace > 0) {
                    sub.setStatus(ST_GRACE);
                    sub.setGraceEndDate(sub.getEndDate().plusDays(grace));
                    subscriptionRepository.save(sub);
                } else {
                    expireSubscription(sub);
                }
            }
        }
        for (CustomerSubscription sub : subscriptionRepository.findByStatus(ST_GRACE)) {
            if (sub.getGraceEndDate() != null && !sub.getGraceEndDate().isBefore(today)) {
                continue;
            }
            expireSubscription(sub);
        }
        for (CustomerSubscription sub : subscriptionRepository.findByStatus(ST_TRIAL)) {
            if (sub.getTrialEndDate() != null && sub.getTrialEndDate().isBefore(today)) {
                // Trial ended without payment → expire commercial access
                expireSubscription(sub);
            }
        }
    }

    private void expireSubscription(CustomerSubscription sub) {
        sub.setStatus(ST_EXPIRED);
        subscriptionRepository.save(sub);
        entitlementService.deactivatePlanModules(sub.getCustomerId(), sub.getPlanId());
    }

    private static LocalDate computeEndDate(LocalDate start, String frequency) {
        return switch (frequency == null ? "" : frequency.toUpperCase()) {
            case "MONTHLY" -> start.plusMonths(1);
            case "QUARTERLY" -> start.plusMonths(3);
            case "HALF_YEARLY", "SEMI_ANNUAL" -> start.plusMonths(6);
            case "ANNUAL" -> start.plusYears(1);
            case "CUSTOM" -> start.plusDays(30);
            default -> start.plusMonths(1);
        };
    }

    private static String sha256(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    /** Minimal JSON string field extraction without full parser dependency for webhook payloads. */

    static String extractNestedNotesPaymentId(String json) {
        if (json == null) return null;
        int notes = json.indexOf("\"notes\"");
        if (notes < 0) return null;
        return extractJsonString(json.substring(notes), "paymentId");
    }
    public static String extractJsonString(String json, String field) {
        if (json == null) return null;
        String key = "\"" + field + "\"";
        int i = json.indexOf(key);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + key.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }

    public record PaymentInitiation(
            String paymentId,
            String invoiceId,
            String gatewayPaymentRef,
            String redirectUrl,
            String keyId,
            long amountSubunits,
            String currencyCode,
            java.math.BigDecimal amount) {}
}
