package com.automotive.costing.billing.gateway;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Dev/test gateway. Production must use a real adapter; webhook still requires HMAC signature.
 */
@Component
public class StubPaymentGateway implements PaymentGateway {

    public static final String CODE = "STUB";
    /** Configurable via env BILLING_STUB_WEBHOOK_SECRET in production-like tests. */
    private final String webhookSecret = System.getenv().getOrDefault(
            "BILLING_STUB_WEBHOOK_SECRET", "stub-webhook-secret-change-me");

    @Override
    public String gatewayCode() {
        return CODE;
    }

    @Override
    public InitiateResult initiatePayment(InitiateRequest request) {
        String ref = "STUB-" + request.paymentId();
        return new InitiateResult(ref, "/billing/stub-complete?paymentId=" + request.paymentId(), "PENDING");
    }

    @Override
    public boolean verifyWebhookSignature(String payload, Map<String, String> headers) {
        String sig = headers.getOrDefault("X-Stub-Signature", headers.getOrDefault("x-stub-signature", ""));
        if (sig.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return expected.equalsIgnoreCase(sig);
        } catch (Exception e) {
            return false;
        }
    }

    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
