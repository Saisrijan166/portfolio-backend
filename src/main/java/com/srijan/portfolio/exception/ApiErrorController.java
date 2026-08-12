package com.srijan.portfolio.exception;

import com.srijan.portfolio.dto.ApiErrorResponse;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Answers the container's error dispatch in the same envelope every other endpoint uses.
 *
 * <p>{@link GlobalExceptionHandler} only sees exceptions raised inside the dispatcher servlet.
 * Anything thrown by a servlet filter, and any response the container produces on its own, forwards
 * to {@code /error} instead — where Boot's default controller returns
 * {@code {"timestamp":…,"status":500,"error":"Internal Server Error","path":…}}. The frontend reads
 * {@code data.error.message}, which is absent from that shape, so it fell back to showing the raw
 * axios text ("Request failed with status code 500"). This keeps the contract consistent.
 */
@Slf4j
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiErrorResponse> handleError(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (status.is5xxServerError()) {
            log.error("Unhandled error dispatch status={} path={}",
                    status.value(), request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        }

        if (exception instanceof Throwable throwable) {
            UserFacingErrors.SafeError safeError = UserFacingErrors.resolve(throwable);
            return ResponseEntity.status(safeError.status())
                    .body(ApiResponses.error(safeError.code(), safeError.message()));
        }

        return ResponseEntity.status(status).body(ApiResponses.error(codeFor(status), messageFor(status)));
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status instanceof Integer code) {
            HttpStatus resolved = HttpStatus.resolve(code);
            if (resolved != null) {
                return resolved;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String codeFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "NOT_FOUND";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            case TOO_MANY_REQUESTS -> "RATE_LIMITED";
            default -> status.is4xxClientError() ? "BAD_REQUEST" : "INTERNAL_SERVER_ERROR";
        };
    }

    private String messageFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "That resource doesn't exist";
            case UNAUTHORIZED -> "Authentication is required to access this resource";
            case FORBIDDEN -> "You do not have permission to access this resource";
            case METHOD_NOT_ALLOWED -> "That action isn't supported on this resource";
            case TOO_MANY_REQUESTS -> "Too many requests. Please try again shortly.";
            default -> status.is4xxClientError()
                    ? "We couldn't process that request. Please check the details and try again."
                    : UserFacingErrors.GENERIC_MESSAGE;
        };
    }
}
