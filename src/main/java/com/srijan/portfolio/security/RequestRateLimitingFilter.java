package com.srijan.portfolio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Per-IP request rate limiting for every API endpoint.
 *
 * <p>Two counters are applied to each request: the endpoint's own limit (see {@link RateLimitPolicy})
 * and an aggregate ceiling across all endpoints, so spreading traffic over many paths no longer
 * multiplies the effective throughput.
 *
 * <p>Registered explicitly in {@code SecurityConfig} ahead of every other custom filter — including
 * the cron API key check — so unauthenticated credential and key guessing is counted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestRateLimitingFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_KEYS = 50_000;
    private static final int EVICTION_INTERVAL = 500;

    private final ObjectMapper objectMapper;
    private final RateLimitPolicy policy;

    private final FixedWindowRateLimiter limiter =
            new FixedWindowRateLimiter(WINDOW, MAX_TRACKED_KEYS, EVICTION_INTERVAL);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || policy.isExempt(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        FixedWindowRateLimiter.Hit hit;
        try {
            hit = evaluate(request);
        } catch (RuntimeException exception) {
            // Rate limiting must never be the reason a request fails.
            log.warn("Rate limiting skipped for path={} due to an internal error", request.getRequestURI(), exception);
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitHeaders.apply(response, hit);

        if (hit.exceeded()) {
            log.warn("Rate limit exceeded ip={} method={} path={}",
                    ClientIpFilter.resolve(request), request.getMethod(), request.getRequestURI());
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Counts the request against both the endpoint rule and the global per-IP ceiling, and returns
     * whichever of the two is closest to its limit so the reported headers are the binding ones.
     */
    private FixedWindowRateLimiter.Hit evaluate(HttpServletRequest request) {
        String clientIp = ClientIpFilter.resolve(request);
        RateLimitPolicy.Rule rule = policy.resolve(request.getMethod(), request.getRequestURI());

        FixedWindowRateLimiter.Hit ruleHit = limiter.hit(clientIp + "|" + rule.bucket(), rule.limit());
        FixedWindowRateLimiter.Hit globalHit = limiter.hit(clientIp + "|*", policy.globalLimit());

        if (globalHit.exceeded() && !ruleHit.exceeded()) {
            return globalHit;
        }
        if (ruleHit.exceeded()) {
            return ruleHit;
        }
        return ruleHit.remaining() <= globalHit.remaining() ? ruleHit : globalHit;
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponses.error(
                "RATE_LIMITED",
                "Too many requests. Please try again shortly."
        ));
    }
}
