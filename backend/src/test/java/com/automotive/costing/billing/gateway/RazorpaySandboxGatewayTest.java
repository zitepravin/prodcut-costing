package com.automotive.costing.billing.gateway;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RazorpaySandboxGatewayTest {
    @Test
    void notConfigured_initiateFails() {
        var g = new RazorpaySandboxGateway("", "", "wh", "https://api.razorpay.com/v1");
        assertFalse(g.isConfigured());
        var ex = assertThrows(BusinessRuleViolationException.class, () -> g.initiatePayment(
                new PaymentGateway.InitiateRequest("p","i", new BigDecimal("10"), "INR", "c", "http://x", Map.of())));
        assertEquals("BILLING-RP-CONFIG", ex.getRuleReference());
    }
    @Test
    void webhookSignature_validInvalid() {
        var g = new RazorpaySandboxGateway("k", "s", "webhook-secret-test", "https://api.razorpay.com/v1");
        String payload = "{\"event\":\"payment.captured\"}";
        String sig = g.signWebhook(payload);
        assertTrue(g.verifyWebhookSignature(payload, Map.of("X-Razorpay-Signature", sig)));
        assertFalse(g.verifyWebhookSignature(payload, Map.of("X-Razorpay-Signature", "bad")));
        assertFalse(g.verifyWebhookSignature(payload, Map.of()));
    }
    @Test
    void subunits() {
        assertEquals(500000L, RazorpaySandboxGateway.toSubunits(new BigDecimal("5000.00")));
        assertEquals(new BigDecimal("5000.0000"), RazorpaySandboxGateway.fromSubunits(500000L));
    }
    @Test
    void code() {
        assertEquals("RAZORPAY", new RazorpaySandboxGateway("a","b","c",null).gatewayCode());
    }
}
