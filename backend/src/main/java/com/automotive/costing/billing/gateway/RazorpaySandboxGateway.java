package com.automotive.costing.billing.gateway;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Component
public class RazorpaySandboxGateway implements PaymentGateway {
    public static final String CODE = "RAZORPAY";
    private static final Logger log = LoggerFactory.getLogger(RazorpaySandboxGateway.class);
    private final String keyId, keySecret, webhookSecret, baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public RazorpaySandboxGateway(
            @Value("${app.billing.razorpay.key-id:}") String keyId,
            @Value("${app.billing.razorpay.key-secret:}") String keySecret,
            @Value("${app.billing.razorpay.webhook-secret:}") String webhookSecret,
            @Value("${app.billing.razorpay.base-url:https://api.razorpay.com/v1}") String baseUrl) {
        this.keyId = keyId == null ? "" : keyId.trim();
        this.keySecret = keySecret == null ? "" : keySecret.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://api.razorpay.com/v1" : baseUrl.replaceAll("/$", "");
    }

    @Override public String gatewayCode() { return CODE; }
    public boolean isConfigured() { return !keyId.isBlank() && !keySecret.isBlank(); }

    @Override
    public InitiateResult initiatePayment(InitiateRequest request) {
        if (!isConfigured()) {
            throw new BusinessRuleViolationException(
                    "Razorpay sandbox not configured (RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET)", "BILLING-RP-CONFIG");
        }
        long amountSubunits = toSubunits(request.amount());
        if (amountSubunits < 100L) {
            throw new BusinessRuleViolationException("Amount must be at least 100 paise", "BILLING-RP-MIN");
        }
        String currency = request.currencyCode() == null ? "INR" : request.currencyCode().toUpperCase();
        String receipt = "r" + request.paymentId().replace("-", "");
        if (receipt.length() > 40) receipt = receipt.substring(0, 40);
        String body = "{\"amount\":%d,\"currency\":\"%s\",\"receipt\":\"%s\",\"notes\":{\"paymentId\":\"%s\",\"invoiceId\":\"%s\",\"customerId\":\"%s\"}}"
                .formatted(amountSubunits, currency, receipt, request.paymentId(),
                        nullToEmpty(request.invoiceId()), nullToEmpty(request.customerId()));
        try {
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/orders")).timeout(Duration.ofSeconds(20))
                    .header("Authorization", basicAuth()).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BusinessRuleViolationException("Razorpay order create HTTP " + resp.statusCode(), "BILLING-RP-ORDER");
            }
            String orderId = extractJsonString(resp.body(), "id");
            if (orderId == null || orderId.isBlank()) {
                throw new BusinessRuleViolationException("Razorpay response missing order id", "BILLING-RP-ORDER");
            }
            String ret = request.returnUrl() == null ? "" : request.returnUrl();
            String sep = ret.contains("?") ? "&" : "?";
            String redirect = ret + sep + "razorpay_order_id=" + orderId + "&paymentId=" + request.paymentId() + "&key_id=" + keyId;
            return new InitiateResult(orderId, redirect, "PENDING", keyId, amountSubunits,
                    request.currencyCode() == null ? "INR" : request.currencyCode().toUpperCase());
        } catch (BusinessRuleViolationException e) { throw e; }
        catch (Exception e) {
            log.warn("Razorpay order error: {}", e.toString());
            throw new BusinessRuleViolationException("Razorpay order create error", "BILLING-RP-ORDER");
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, Map<String, String> headers) {
        if (webhookSecret.isBlank()) return false;
        String sig = header(headers, "X-Razorpay-Signature");
        if (sig == null || sig.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return constantTimeEquals(HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))), sig);
        } catch (Exception e) { return false; }
    }

    @Override
    public Optional<VerificationResult> verifyPayment(VerificationRequest request) {
        if (!isConfigured()) return Optional.of(new VerificationResult(false, null, "CONFIG", null, null, "not configured"));
        if (request.providerPaymentId() == null || request.providerPaymentId().isBlank()) {
            return Optional.of(new VerificationResult(false, null, "MISSING", null, null, "providerPaymentId required"));
        }
        try {
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/payments/" + request.providerPaymentId()))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", basicAuth()).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                return Optional.of(new VerificationResult(false, request.providerPaymentId(), "HTTP_" + resp.statusCode(), null, null, "fetch failed"));
            }
            String status = extractJsonString(resp.body(), "status");
            String currency = extractJsonString(resp.body(), "currency");
            String amountStr = extractJsonNumber(resp.body(), "amount");
            BigDecimal amount = amountStr == null ? null : fromSubunits(Long.parseLong(amountStr));
            boolean ok = "captured".equalsIgnoreCase(status) || "authorized".equalsIgnoreCase(status);
            if (ok && request.expectedAmount() != null && amount != null
                    && amount.compareTo(request.expectedAmount().setScale(4, RoundingMode.HALF_UP)) != 0) {
                return Optional.of(new VerificationResult(false, request.providerPaymentId(), status, amount, currency, "AMOUNT_MISMATCH"));
            }
            if (ok && request.expectedCurrency() != null && currency != null
                    && !request.expectedCurrency().equalsIgnoreCase(currency)) {
                return Optional.of(new VerificationResult(false, request.providerPaymentId(), status, amount, currency, "CURRENCY_MISMATCH"));
            }
            return Optional.of(new VerificationResult(ok, request.providerPaymentId(), status, amount, currency, ok ? null : "status=" + status));
        } catch (Exception e) {
            return Optional.of(new VerificationResult(false, request.providerPaymentId(), "ERROR", null, null, e.toString()));
        }
    }

    @Override
    public Optional<RefundResult> createRefund(RefundRequest request) {
        if (!isConfigured()) return Optional.of(new RefundResult(false, null, "CONFIG", "not configured"));
        String pid = request.providerPaymentId() != null ? request.providerPaymentId() : request.gatewayPaymentRef();
        if (pid == null || pid.isBlank()) return Optional.of(new RefundResult(false, null, "MISSING", "payment id required"));
        try {
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/payments/" + pid + "/refund")).timeout(Duration.ofSeconds(20))
                    .header("Authorization", basicAuth()).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"amount\":" + toSubunits(request.amount()) + "}")).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                return Optional.of(new RefundResult(false, null, "HTTP_" + resp.statusCode(), "refund failed"));
            }
            return Optional.of(new RefundResult(true, extractJsonString(resp.body(), "id"),
                    extractJsonString(resp.body(), "status"), null));
        } catch (Exception e) {
            return Optional.of(new RefundResult(false, null, "ERROR", e.toString()));
        }
    }

    public String signWebhook(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }


    @Override
    public boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) {
        if (keySecret.isBlank() || orderId == null || paymentId == null || signature == null || signature.isBlank()) {
            return false;
        }
        String payload = orderId + "|" + paymentId;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return constantTimeEquals(expected, signature);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<String> publicKeyId() {
        return keyId.isBlank() ? Optional.empty() : Optional.of(keyId);
    }

    private String basicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
    }
    static long toSubunits(BigDecimal amount) {
        if (amount == null) throw new BusinessRuleViolationException("Amount required", "BILLING-RP-AMT");
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
    static BigDecimal fromSubunits(long subunits) {
        return BigDecimal.valueOf(subunits).movePointLeft(2).setScale(4, RoundingMode.HALF_UP);
    }
    static String extractJsonString(String json, String field) {
        if (json == null) return null;
        String key = "\"" + field + "\"";
        int i = json.indexOf(key); if (i < 0) return null;
        int colon = json.indexOf(':', i + key.length()); if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1); if (start < 0) return null;
        int end = json.indexOf('"', start + 1); if (end < 0) return null;
        return json.substring(start + 1, end);
    }
    static String extractJsonNumber(String json, String field) {
        if (json == null) return null;
        String key = "\"" + field + "\"";
        int i = json.indexOf(key); if (i < 0) return null;
        int colon = json.indexOf(':', i + key.length()); if (colon < 0) return null;
        int j = colon + 1; while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
        int k = j; while (k < json.length() && (Character.isDigit(json.charAt(k)) || json.charAt(k) == '.' || json.charAt(k) == '-')) k++;
        return k == j ? null : json.substring(j, k);
    }
    private static String header(Map<String, String> headers, String name) {
        if (headers == null) return null;
        if (headers.containsKey(name)) return headers.get(name);
        for (var e : headers.entrySet()) if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) return e.getValue();
        return null;
    }
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0; for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i); return r == 0;
    }
    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
