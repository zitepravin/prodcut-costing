package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * SEC-06: "System shall enforce configurable password complexity and expiry policy for
 * accounts not using SSO." The complexity rule below (10+ chars, upper, lower, digit,
 * special character) is an MVP default, not a client-confirmed policy - same pattern as
 * MaterialMasterService's 10% variance threshold and ShouldCostService's 90-day staleness
 * window. Expiry enforcement (the other half of SEC-06) is NOT implemented in this
 * session - {@code User.passwordUpdatedAt} is captured so a future expiry check has the
 * data it needs, but nothing currently reads it to force a reset. Flagged as a follow-up
 * in NEXT_AGENT.md, not silently incomplete.
 */
@Component
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 10;
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new BusinessRuleViolationException(
                    "Password must be at least " + MIN_LENGTH + " characters.", "SEC-06-password-policy");
        }
        if (!UPPER.matcher(rawPassword).find() || !LOWER.matcher(rawPassword).find()
                || !DIGIT.matcher(rawPassword).find() || !SPECIAL.matcher(rawPassword).find()) {
            throw new BusinessRuleViolationException(
                    "Password must include at least one uppercase letter, one lowercase letter, "
                            + "one digit, and one special character.", "SEC-06-password-policy");
        }
    }
}
