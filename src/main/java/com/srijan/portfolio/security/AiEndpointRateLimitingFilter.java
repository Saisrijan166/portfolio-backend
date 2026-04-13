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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AiEndpointRateLimitingFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int IP_LIMIT = 12;
    private static final int USER_LIMIT = 8;

    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${security.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !(path.startsWith("/api/me/resume/parse")
                || path.startsWith("/api/me/resume/score")
                || path.startsWith("/api/me/resume/regenerate")
                || path.startsWith("/api/ai/summarize-section"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        evictExpiredBuckets();

        if (!consume("ip:" + resolveClientIp(request), IP_LIMIT) || !consume("user:" + resolveUsername(), USER_LIMIT)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponses.error(
                    "AI_RATE_LIMITED",
                    "AI request limit exceeded. Please wait a minute and try again."
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean consume(String key, int capacity) {
        Bucket bucket = buckets.compute(key, (ignored, existing) -> existing == null || existing.isExpired()
                ? new Bucket(capacity)
                : existing);
        return bucket.consumeIfAvailable();
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = trustProxyHeaders ? request.getHeader("X-Forwarded-For") : null;
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void evictExpiredBuckets() {
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static final class Bucket {
        private final Instant createdAt = Instant.now();
        private int remainingTokens;

        private Bucket(int remainingTokens) {
            this.remainingTokens = remainingTokens;
        }

        private synchronized boolean consumeIfAvailable() {
            if (remainingTokens <= 0) {
                return false;
            }
            remainingTokens -= 1;
            return true;
        }

        private boolean isExpired() {
            return createdAt.plus(WINDOW).isBefore(Instant.now());
        }
    }
}
