package com.automotive.costing.billing.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RazorpayCheckoutSignatureTest {

    @Test
    void checkoutSignature_valid() throws Exception {
        RazorpaySandboxGateway g = new RazorpaySandboxGateway(
                "rzp_test_x", "test_secret_key", "whsec", "https://api.razorpay.com/v1");
        String orderId = "order_ABC";
        String paymentId = "pay_XYZ";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(
                "test_secret_key".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String sig = java.util.HexFormat.of().formatHex(
                mac.doFinal((orderId + "|" + paymentId).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertTrue(g.verifyCheckoutSignature(orderId, paymentId, sig));
        assertFalse(g.verifyCheckoutSignature(orderId, paymentId, "deadbeef"));
    }

    @Test
    void publicKeyId_exposedWithoutSecret() {
        RazorpaySandboxGateway g = new RazorpaySandboxGateway(
                "rzp_test_public", "secret", "", "https://api.razorpay.com/v1");
        assertEquals("rzp_test_public", g.publicKeyId().orElseThrow());
    }
}
