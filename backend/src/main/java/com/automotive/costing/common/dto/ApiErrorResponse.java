package com.automotive.costing.common.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response shape for every non-2xx API response, per API_SPEC.md Section 1.
 * Never expose stack traces or internal implementation details to clients.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ApiErrorResponse of(int status, String error, String message, String path,
                                       List<FieldError> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}
