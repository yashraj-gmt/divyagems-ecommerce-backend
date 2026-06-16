package com.divyagems.ecommerce.exception;

import com.divyagems.ecommerce.common.StandardApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

/**
 * Centralized exception handler for all REST controllers.
 * Returns {@link StandardApiResponse} with appropriate HTTP status for every error type.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── Domain Exceptions ─────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(StandardApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violated: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleBadRequestException(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<StandardApiResponse<Void>> handlePaymentVerificationException(PaymentVerificationException ex) {
        log.warn("Payment verification failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardApiResponse.error(ex.getMessage()));
    }

    // ─── Validation Exceptions ─────────────────────────────────

    /**
     * Handles @Valid failures on @RequestBody DTOs.
     * Returns a list of field-level errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<StandardApiResponse.FieldErrorDetails> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(StandardApiResponse.FieldErrorDetails::from)
                .toList();
        log.debug("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(StandardApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardApiResponse.error("Required parameter missing: " + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = String.format("Parameter '%s' must be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(StandardApiResponse.error(msg));
    }

    // ─── Security Exceptions ───────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(StandardApiResponse.error("You don't have permission"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        if (ex instanceof BadCredentialsException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(StandardApiResponse.error("Invalid email or password"));
        } else if (ex instanceof DisabledException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(StandardApiResponse.error("Your account has been disabled. Please contact support."));
        } else if (ex instanceof LockedException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(StandardApiResponse.error("Your account is locked. Please contact support."));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(StandardApiResponse.error("Authentication required"));
    }

    // ─── Infrastructure Exceptions ─────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(StandardApiResponse.error("Resource already exists"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<StandardApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(StandardApiResponse.error("File too large. Maximum allowed size is 10 MB."));
    }

    // ─── Catch-All ─────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(StandardApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
