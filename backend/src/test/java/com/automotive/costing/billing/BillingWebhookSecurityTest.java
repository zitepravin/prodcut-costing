package com.automotive.costing.billing;

import com.automotive.costing.billing.gateway.StubPaymentGateway;
import com.automotive.costing.billing.service.BillingSubscriptionService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BillingWebhookSecurityTest {

    @Test
    void stubGatewayRejectsMissingSignature() {
        StubPaymentGateway g = new StubPaymentGateway();
        assertFalse(g.verifyWebhookSignature("{\"paymentId\":\"x\"}", Map.of()));
    }

    @Test
    void stubGatewayAcceptsValidHmac() {
        StubPaymentGateway g = new StubPaymentGateway();
        String payload = "{\"paymentId\":\"pay-1\",\"event\":\"paid\"}";
        String sig = g.sign(payload);
        assertTrue(g.verifyWebhookSignature(payload, Map.of("X-Stub-Signature", sig)));
    }

    @Test
    void extractJsonStringReadsField() {
        String json = "{\"paymentId\":\"abc-123\",\"reason\":\"insufficient_funds\"}";
        assertEquals("abc-123", BillingSubscriptionService.extractJsonString(json, "paymentId"));
        assertEquals("insufficient_funds", BillingSubscriptionService.extractJsonString(json, "reason"));
        assertNull(BillingSubscriptionService.extractJsonString(json, "missing"));
    }
}
