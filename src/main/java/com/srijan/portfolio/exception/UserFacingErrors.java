package com.srijan.portfolio.exception;

import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

/**
 * Turns any thrown exception into something safe to show a person.
 *
 * <p>The rule this enforces: a message is only sent to a client if we wrote it. Framework, driver
 * and library messages ("FATAL: too many connections for role...", "Connection is not available,
 * request timed out after 20000ms") are diagnostics — useful in logs, meaningless and alarming in a
 * login form, and they disclose internals. Every other path resolves to a fixed message here.
 *
 * <p>Used by {@link GlobalExceptionHandler} for API responses and by the OAuth handlers, which
 * cannot go through the controller advice because they run inside the security filter chain.
 */
public final class UserFacingErrors {

    /** A response that is safe to serialise to a client. */
    public record SafeError(HttpStatus status, String code, String message) {
    }

    public static final String GENERIC_MESSAGE =
            "Something went wrong on our side. Please try again in a few moments.";

    private static final SafeError GENERIC =
            new SafeError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", GENERIC_MESSAGE);

    private static final SafeError DATABASE = new SafeError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "DATABASE_UNAVAILABLE",
            "We're having trouble reaching our database right now. Please try again in a few moments."
    );

    private static final SafeError EMAIL = new SafeError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "EMAIL_DELIVERY_FAILED",
            "We couldn't send that email right now. Please try again in a few moments."
    );

    private static final SafeError UPSTREAM = new SafeError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "UPSTREAM_UNAVAILABLE",
            "A service we depend on isn't responding right now. Please try again in a few moments."
    );

    /** How far to walk the cause chain. Guards against a self-referencing chain. */
    private static final int MAX_CAUSE_DEPTH = 12;

    private UserFacingErrors() {
    }

    /**
     * Classifies a throwable — including anything in its cause chain — into a safe response.
     * Falls back to a generic message rather than echoing an unrecognised exception.
     */
    public static SafeError resolve(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;

        while (current != null && depth++ < MAX_CAUSE_DEPTH) {
            // Our own exceptions carry messages that were written for users.
            if (current instanceof ApiException apiException) {
                return new SafeError(apiException.getStatus(), apiException.getCode(), apiException.getMessage());
            }
            if (isDatabaseFailure(current)) {
                return DATABASE;
            }
            if (current instanceof MailException) {
                return EMAIL;
            }
            if (isUpstreamFailure(current)) {
                return UPSTREAM;
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }

        return GENERIC;
    }

    /** Convenience for callers that only need the text. */
    public static String messageFor(Throwable throwable) {
        return resolve(throwable).message();
    }

    private static boolean isDatabaseFailure(Throwable throwable) {
        if (throwable instanceof DataIntegrityViolationException) {
            // A rejected write, not an unreachable database — let the dedicated handler answer.
            return false;
        }
        return throwable instanceof DataAccessException
                || throwable instanceof TransactionException
                || throwable instanceof SQLException
                || throwable instanceof PersistenceException;
    }

    private static boolean isUpstreamFailure(Throwable throwable) {
        return throwable instanceof ConnectException
                || throwable instanceof UnknownHostException
                || throwable instanceof SocketTimeoutException
                || throwable instanceof TimeoutException
                || throwable instanceof ResourceAccessException
                // Broad, so it is checked last: the AI and email clients surface transport failures
                // as plain IOExceptions.
                || throwable instanceof IOException;
    }
}
