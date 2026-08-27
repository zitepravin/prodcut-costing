package com.automotive.costing.common.exception;

/**
 * Thrown when an operation would violate a uniqueness business rule
 * (e.g. MDM-01: every master record code must be unique and non-duplicable).
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
