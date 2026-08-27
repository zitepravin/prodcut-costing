package com.automotive.costing.common.exception;

import com.automotive.costing.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Single, centralized place where every exception in the system is translated into the
 * standard ApiErrorResponse shape (API_SPEC.md Section 1). Controllers and services must
 * never write their own try/catch-and-format-response logic; throw a typed exception and
 * let this handler do the translation. Never leak stack traces or internal details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                               HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                "One or more fields failed validation", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                             HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex,
                                                              HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(), "DUPLICATE_RESOURCE", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleViolationException ex,
                                                                 HttpServletRequest request) {
        String rule = ex.getRuleReference();
        String message = rule != null
                ? ex.getMessage() + " [rule: " + rule + "]"
                : ex.getMessage();
        // Central Access denials (module / tenant) must be HTTP 403, not 422
        if (rule != null && (rule.startsWith("ACCESS-") || rule.startsWith("LICENSE_")
                || rule.endsWith("_LIMIT_EXCEEDED") || "ROLE_NOT_ASSIGNABLE".equals(rule)
                || "PERMISSION_NOT_ENTITLED".equals(rule))) {
            String errorCode = switch (rule) {
                case "ACCESS-02", "ACCESS-03", "ACCESS-04", "ACCESS-05" -> "MODULE_NOT_ENABLED";
                case "ACCESS-06", "ACCESS-07", "ACCESS-TENANT-01", "ACCESS-TENANT-02" -> "TENANT_ACCESS_DENIED";
                case "ACCESS-FEAT-01", "ACCESS-FEAT-02", "ACCESS-FEAT-03", "ACCESS-FEAT-04" -> "FEATURE_NOT_ENABLED";
                default -> rule; // LICENSE_*, *_LIMIT_EXCEEDED, ROLE_NOT_ASSIGNABLE, etc.
            };
            ApiErrorResponse body = ApiErrorResponse.of(
                    HttpStatus.FORBIDDEN.value(), errorCode, message, request.getRequestURI());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
        ApiErrorResponse body = ApiErrorResponse.of(
                422, "BUSINESS_RULE_VIOLATION", message, request.getRequestURI());
        return ResponseEntity.status(422).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                                 HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED",
                "You do not have permission to perform this action", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                    HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "INVALID_ARGUMENT", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.toString(), ex);
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support if this persists.",
                request.getRequestURI());
        return ResponseEntity.internalServerError().body(body);
    }

    private ApiErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
    }
}
