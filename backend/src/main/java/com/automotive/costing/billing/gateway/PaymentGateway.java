package com.automotive.costing.billing.gateway;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Payment gateway port — adapters implement provider-specific APIs.
 * Application code must not depend on a single provider.
 * KEY_SECRET must never be returned to the frontend.
 */
public interface PaymentGateway {

    String gatewayCode();

    InitiateResult initiatePayment(InitiateRequest request);

    boolean verifyWebhookSignature(String payload, Map<String, String> headers);

    /**
     * Razorpay Standard Checkout client signature:
     * HMAC_SHA256(order_id + "|" + razorpay_payment_id, key_secret)
     */
    default boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) {
        return false;
    }

    /** Public Key ID only — safe for Checkout.js options.key */
    default Optional<String> publicKeyId() {
        return Optional.empty();
    }

    default Optional<VerificationResult> verifyPayment(VerificationRequest request) {
        return Optional.empty();
    }

    default Optional<RefundResult> createRefund(RefundRequest request) {
        return Optional.empty();
    }

    record InitiateRequest(
            String paymentId,
            String invoiceId,
            BigDecimal amount,
            String currencyCode,
            String customerId,
            String returnUrl,
            Map<String, String> metadata) {}

    record InitiateResult(
            String gatewayPaymentRef,
            String redirectUrl,
            String status,
            String publicKeyId,
            long amountSubunits,
            String currencyCode) {
        public InitiateResult(String gatewayPaymentRef, String redirectUrl, String status) {
            this(gatewayPaymentRef, redirectUrl, status, null, 0L, null);
        }
    }

    record VerificationRequest(
            String paymentId,
            String gatewayPaymentRef,
            String providerPaymentId,
            BigDecimal expectedAmount,
            String expectedCurrency) {}

    record VerificationResult(
            boolean success,
            String providerPaymentId,
            String status,
            BigDecimal amount,
            String currencyCode,
            String failureReason) {}

    record RefundRequest(
            String gatewayPaymentRef,
            String providerPaymentId,
            BigDecimal amount,
            String currencyCode,
            String reason) {}

    record RefundResult(
            boolean success,
            String providerRefundId,
            String status,
            String failureReason) {}
}
