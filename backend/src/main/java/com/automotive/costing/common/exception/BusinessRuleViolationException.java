package com.automotive.costing.common.exception;

/**
 * Thrown when a request is well-formed and authorized but violates a documented
 * business rule from the SRS (e.g. "a rate cannot be deleted once referenced in
 * an approved cost estimate; it can only be deactivated" - SRS Section 4.1.5).
 * Maps to HTTP 422 Unprocessable Entity.
 *
 * The rule reference is included in {@link #getMessage()} so API clients and
 * unit tests can assert on the SRS/TDD rule id without inspecting a separate field.
 */
public class BusinessRuleViolationException extends RuntimeException {

    private final String ruleReference;

    public BusinessRuleViolationException(String message, String ruleReference) {
        super(formatMessage(message, ruleReference));
        this.ruleReference = ruleReference;
    }

    public String getRuleReference() {
        return ruleReference;
    }

    private static String formatMessage(String message, String ruleReference) {
        if (ruleReference == null || ruleReference.isBlank()) {
            return message;
        }
        if (message != null && message.contains(ruleReference)) {
            return message;
        }
        return message + " [" + ruleReference + "]";
    }
}
