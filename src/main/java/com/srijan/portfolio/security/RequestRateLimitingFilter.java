package com.srijan.portfolio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class RequestRateLimitingFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int AUTH_LIMIT = 30;
    private static final int PUBLIC_FEEDBACK_LIMIT = 15;
    private static final int ADMIN_FEEDBACK_LIMIT = 40;
    private static final int PUBLIC_LIMIT = 200;
    private static final int DEFAULT_LIMIT = 300;

    private final ObjectMapper objectMapper;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Value("${security.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = resolveLimit(path);
        String key = clientKey(request) + ":" + normalizePath(path);

        WindowCounter counter = counters.compute(key, (ignored, existing) -> existing == null || existing.isExpired()
                ? new WindowCounter()
                : existing);

        if (counter.incrementAndGet() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponses.error(
                    "RATE_LIMITED",
                    "Too many requests. Please try again shortly."
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int resolveLimit(String path) {
        if (path.startsWith("/api/auth/")) {
            return AUTH_LIMIT;
        }
        if (isPublicFeedbackPath(path)) {
            return PUBLIC_FEEDBACK_LIMIT;
        }
        if (path.startsWith("/api/me/feedback")) {
            return ADMIN_FEEDBACK_LIMIT;
        }
        if (path.startsWith("/api/public/")) {
            return PUBLIC_LIMIT;
        }
        return DEFAULT_LIMIT;
    }

    private String normalizePath(String path) {
        if (isPublicFeedbackPath(path)) {
            if (path.endsWith("/feedback/platform")) {
                return "/api/public/portfolio/{username}/feedback/platform";
            }
            return "/api/public/portfolio/{username}/feedback";
        }
        return path;
    }

    private boolean isPublicFeedbackPath(String path) {
        return path.matches("^/api/public/portfolio/[^/]+/feedback(?:/platform)?$");
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = trustProxyHeaders ? request.getHeader("X-Forwarded-For") : null;
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class WindowCounter {
        private final Instant startedAt = Instant.now();
        private final AtomicInteger count = new AtomicInteger(0);

        boolean isExpired() {
            return startedAt.plus(WINDOW).isBefore(Instant.now());
        }

        int incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
