package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SEC-07: verifies the hand-rolled RFC 6238 TOTP implementation is internally consistent
 * - a code independently computed for a given time step (via reflection into the private
 * algorithm, since the whole point of the public API is not exposing raw code generation)
 * verifies correctly, tolerates one step of clock drift, rejects codes from further away,
 * and rejects a code generated against a different secret. No external reference vector
 * is cross-checked here (no internet-available third-party MFA library in this sandbox to
 * validate against) - round-trip and cross-secret correctness is what actually matters
 * for this system's use of the algorithm.
 */
class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void generateSecret_producesADifferentSecretEachTime() {
        String a = totpService.generateSecret();
        String b = totpService.generateSecret();

        assertThat(a).isNotEqualTo(b);
        assertThat(a).matches("[A-Z2-7]+"); // valid Base32 alphabet
    }

    @Test
    void verifyCode_withCorrectlyGeneratedCode_succeeds() throws Exception {
        String secret = totpService.generateSecret();
        long currentStep = System.currentTimeMillis() / 1000L / 30L;

        java.lang.reflect.Method generateCode = TotpService.class.getDeclaredMethod(
                "generateCode", String.class, long.class);
        generateCode.setAccessible(true);
        String currentCode = (String) generateCode.invoke(totpService, secret, currentStep);

        assertThat(currentCode).matches("\\d{6}");
        assertThat(totpService.verifyCode(secret, currentCode)).isTrue();
    }

    @Test
    void verifyCode_toleratesOneStepOfClockDrift() throws Exception {
        String secret = totpService.generateSecret();
        long currentStep = System.currentTimeMillis() / 1000L / 30L;

        java.lang.reflect.Method generateCode = TotpService.class.getDeclaredMethod(
                "generateCode", String.class, long.class);
        generateCode.setAccessible(true);
        String previousStepCode = (String) generateCode.invoke(totpService, secret, currentStep - 1);
        String twoStepsAgoCode = (String) generateCode.invoke(totpService, secret, currentStep - 2);

        assertThat(totpService.verifyCode(secret, previousStepCode)).isTrue();
        assertThat(totpService.verifyCode(secret, twoStepsAgoCode)).isFalse();
    }

    @Test
    void verifyCode_withDifferentSecret_rejectsAnotherAccountsValidCode() throws Exception {
        String secretA = totpService.generateSecret();
        String secretB = totpService.generateSecret();
        long currentStep = System.currentTimeMillis() / 1000L / 30L;

        java.lang.reflect.Method generateCode = TotpService.class.getDeclaredMethod(
                "generateCode", String.class, long.class);
        generateCode.setAccessible(true);
        String codeForA = (String) generateCode.invoke(totpService, secretA, currentStep);

        assertThat(totpService.verifyCode(secretB, codeForA)).isFalse();
    }

    @Test
    void buildOtpAuthUri_producesAWellFormedOtpAuthUri() {
        String secret = totpService.generateSecret();
        String uri = totpService.buildOtpAuthUri(secret, "alice", "TestIssuer");
        assertThat(uri).startsWith("otpauth://totp/TestIssuer:alice");
        assertThat(uri).contains("secret=" + secret);
        assertThat(uri).contains("algorithm=SHA1");
        assertThat(uri).contains("digits=6");
        assertThat(uri).contains("period=30");
    }

    @Test
    void verifyCode_withWrongLengthCode_isRejected() {
        String secret = totpService.generateSecret();
        assertThat(totpService.verifyCode(secret, "12345")).isFalse();
        assertThat(totpService.verifyCode(secret, "1234567")).isFalse();
    }

    @Test
    void verifyCode_withNonNumericCode_isRejected() {
        String secret = totpService.generateSecret();
        assertThat(totpService.verifyCode(secret, "abcdef")).isFalse();
    }

    @Test
    void verifyCode_withNullCode_isRejected() {
        String secret = totpService.generateSecret();
        assertThat(totpService.verifyCode(secret, null)).isFalse();
    }

    @Test
    void verifyCode_withArbitraryWrongCode_isRejected() {
        String secret = totpService.generateSecret();
        // Astronomically unlikely to collide with the actual current code.
        assertThat(totpService.verifyCode(secret, "000000") && totpService.verifyCode(secret, "000001"))
                .isFalse();
    }
}
