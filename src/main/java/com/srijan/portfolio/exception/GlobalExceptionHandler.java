package com.srijan.portfolio.exception;

import com.srijan.portfolio.dto.ApiErrorResponse;
import com.srijan.portfolio.util.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
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
        // The framework message embeds the identifier that was looked up; keep it in the log only.
        log.info("User lookup failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "We couldn't find an account for those details", false);
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

        // Spring Security copies the raw cause message onto this exception (a driver or pool error
        // when the database is unreachable). Classify the cause instead of echoing it.
        log.error("Authentication failed due to an internal error", ex);
        return safeErrorResponse(ex);
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

    // -------------------------------------------------------------------------
    // Infrastructure failures — the database, the mail server, or an upstream API
    // is unreachable. These used to fall through to the catch-all below, which told
    // the user nothing about whether retrying was worthwhile.
    // -------------------------------------------------------------------------

    @ExceptionHandler({DataAccessException.class, TransactionException.class, SQLException.class})
    public ResponseEntity<ApiErrorResponse> handleDataAccess(Exception ex) {
        log.error("Database access failure", ex);
        return safeErrorResponse(ex);
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ApiErrorResponse> handleMailFailure(MailException ex) {
        log.error("Email delivery failure", ex);
        return safeErrorResponse(ex);
    }

    // -------------------------------------------------------------------------
    // Malformed requests. Without these the catch-all reported 500 for what are
    // plainly client mistakes, so callers could not tell the two apart.
    // -------------------------------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "That action isn't supported on this resource", false);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "That content type isn't supported", false);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiErrorResponse> handleMissingRequestPart(Exception ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Some required information is missing from the request", false);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "'" + ex.getName() + "' is not in the expected format", false);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "That file is too large. Please upload a smaller file", false);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNoHandler(Exception ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "That resource doesn't exist", false);
    }

    /**
     * Method-level security denials are thrown inside the dispatcher, so this advice sees them
     * before the security filter chain's access denied handler can. Without this they were reported
     * as 500s.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to access this resource", false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        // Classifies the cause chain: an infrastructure failure wrapped in an application
        // exception still gets its specific message, everything else gets the generic one.
        return safeErrorResponse(ex);
    }

    /**
     * Builds a response from {@link UserFacingErrors}, which never echoes a framework, driver, or
     * third-party message back to the caller.
     */
    private ResponseEntity<ApiErrorResponse> safeErrorResponse(Throwable throwable) {
        UserFacingErrors.SafeError safeError = UserFacingErrors.resolve(throwable);
        return ResponseEntity.status(safeError.status())
                .body(ApiResponses.error(safeError.code(), safeError.message()));
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String code, String message, boolean logAsError) {
        if (logAsError) {
            log.error("API error status={} code={} message={}", status.value(), code, message);
        }
        return ResponseEntity.status(status).body(ApiResponses.error(code, message));
    }
}
