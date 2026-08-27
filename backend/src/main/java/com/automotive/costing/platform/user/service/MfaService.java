package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SEC-07 MFA enrollment lifecycle. Two-step by design (enroll -&gt; confirm) rather than
 * enabling MFA the instant a secret is generated: a user must prove they've actually
 * captured the secret in their authenticator app (by submitting one valid code) before
 * their account starts requiring it at every login, so a display glitch or a missed QR
 * scan can't lock someone out of their own account on the very next login attempt.
 */
@Service
@RequiredArgsConstructor
public class MfaService {

    private static final String ISSUER = "ProductCostingSoftware";

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final AuditService auditService;

    public record EnrollResponse(String secret, String otpAuthUri) {
    }

    /** Step 1: generates and stores a new secret (mfaEnabled stays false until confirmed). */
    @Transactional
    public EnrollResponse enroll(String userId) {
        User user = getOrThrow(userId);
        String secret = totpService.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);
        String uri = totpService.buildOtpAuthUri(secret, user.getUsername(), ISSUER);
        return new EnrollResponse(secret, uri);
    }

    /** Step 2: proves possession of the secret with one valid code, then flips mfaEnabled on. */
    @Transactional
    public void confirm(String userId, String code) {
        User user = getOrThrow(userId);
        if (user.getMfaSecret() == null) {
            throw new BusinessRuleViolationException(
                    "No MFA enrollment in progress - call enroll first.", "SEC-07-mfa-not-enrolled");
        }
        if (!totpService.verifyCode(user.getMfaSecret(), code)) {
            throw new BusinessRuleViolationException("Invalid MFA code.", "SEC-07-mfa-invalid-code");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        auditService.recordUpdate("User", userId, null, "mfaEnabled=true");
    }

    @Transactional
    public void disable(String userId) {
        User user = getOrThrow(userId);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        auditService.recordUpdate("User", userId, null, "mfaEnabled=false");
    }

    private User getOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}
