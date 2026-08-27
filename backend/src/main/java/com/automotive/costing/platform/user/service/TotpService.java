package com.automotive.costing.platform.user.service;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * SEC-07: "System shall support multi-factor authentication for accounts not using SSO."
 * A minimal RFC 6238 TOTP (Time-based One-Time Password) implementation - the same
 * algorithm Google Authenticator, Authy, and every standard authenticator app use -
 * written against only the JDK's {@code javax.crypto} so it doesn't need a new Maven
 * dependency. Secrets are Base32-encoded (the standard TOTP convention, needed so
 * authenticator apps can accept the secret when a user scans/enters it) using a small
 * hand-rolled encoder/decoder since the JDK has no built-in Base32 support (only Base64).
 */
@Component
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES = 20; // 160 bits, standard for HMAC-SHA1-based TOTP
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    /** Accept a code from one step before/after "now" to tolerate normal clock drift. */
    private static final int ALLOWED_WINDOW_STEPS = 1;

    private final SecureRandom secureRandom = new SecureRandom();

    /** Generates a new random Base32 secret for MFA enrollment. */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** The otpauth:// URI an authenticator app can consume directly (typically rendered as a QR code by the frontend). */
    public String buildOtpAuthUri(String secret, String accountLabel, String issuer) {
        return "otpauth://totp/" + urlEncode(issuer) + ":" + urlEncode(accountLabel)
                + "?secret=" + secret + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + CODE_DIGITS + "&period=" + TIME_STEP_SECONDS;
    }

    /** Verifies a 6-digit code against the secret, tolerating +/-1 time step of clock drift. */
    public boolean verifyCode(String secret, String code) {
        if (code == null || !code.matches("\\d{" + CODE_DIGITS + "}")) {
            return false;
        }
        long currentStep = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (int drift = -ALLOWED_WINDOW_STEPS; drift <= ALLOWED_WINDOW_STEPS; drift++) {
            if (code.equals(generateCode(secret, currentStep + drift))) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(String base32Secret, long timeStep) {
        try {
            byte[] key = base32Decode(base32Secret);
            byte[] data = new byte[8];
            long value = timeStep;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute TOTP code", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                result.append(BASE32_ALPHABET.charAt(index));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_ALPHABET.charAt(index));
        }
        return result.toString();
    }

    private byte[] base32Decode(String base32) {
        String clean = base32.trim().toUpperCase().replace("=", "");
        int buffer = 0;
        int bitsLeft = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : clean.toCharArray()) {
            int index = BASE32_ALPHABET.indexOf(c);
            if (index < 0) {
                continue;
            }
            buffer = (buffer << 5) | index;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
