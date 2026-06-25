package com.srijan.portfolio.exception;

import com.srijan.portfolio.dto.ApiErrorResponse;
import com.srijan.portfolio.util.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
        return buildErrorResponse(ex.getStatus(), ex.getCode(), ex.getMessage(), false);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), false);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UsernameNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage(), false);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email, username, or password", false);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "Account is suspended", false);
    }

    @ExceptionHandler(org.springframework.security.authentication.InternalAuthenticationServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleInternalAuth(org.springframework.security.authentication.InternalAuthenticationServiceException ex) {
        if (ex.getCause() instanceof DisabledException) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "Account is suspended", false);
        }

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please try again later.", true);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, false);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "Request body is malformed or has invalid field types", false);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, false);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), false);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return buildErrorResponse(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Request conflicts with existing data", false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please try again later.", true);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String code, String message, boolean logAsError) {
        if (logAsError) {
            log.error("API error status={} code={} message={}", status.value(), code, message);
        }
        return ResponseEntity.status(status).body(ApiResponses.error(code, message));
    }
}
