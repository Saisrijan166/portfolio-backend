package com.srijan.portfolio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Extra throttling for the endpoints that call out to an AI provider, on top of the generic
 * per-endpoint limits in {@link RequestRateLimitingFilter}. These requests cost real money and take
 * seconds of a small thread pool, so they get both a per-IP and a per-identity budget.
 *
 * <p>Registered after {@code JwtAuthenticationFilter} in {@code SecurityConfig} so the authenticated
 * principal is available; unauthenticated callers are bucketed per IP rather than sharing one
 * global anonymous bucket.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiEndpointRateLimitingFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_KEYS = 20_000;
    private static final int EVICTION_INTERVAL = 200;

    private final ObjectMapper objectMapper;

    @Value("${security.rate-limit.ai-per-ip:12}")
    private int ipLimit;

    @Value("${security.rate-limit.ai-per-user:8}")
    private int userLimit;

    private final FixedWindowRateLimiter limiter =
            new FixedWindowRateLimiter(WINDOW, MAX_TRACKED_KEYS, EVICTION_INTERVAL);

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
        FixedWindowRateLimiter.Hit hit;
        try {
            hit = evaluate(request);
        } catch (RuntimeException exception) {
            log.warn("AI rate limiting skipped for path={} due to an internal error", request.getRequestURI(), exception);
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitHeaders.apply(response, hit);

        if (hit.exceeded()) {
            log.warn("AI rate limit exceeded ip={} path={}", ClientIpFilter.resolve(request), request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), ApiResponses.error(
                    "AI_RATE_LIMITED",
                    "AI request limit exceeded. Please wait a minute and try again."
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private FixedWindowRateLimiter.Hit evaluate(HttpServletRequest request) {
        String clientIp = ClientIpFilter.resolve(request);

        FixedWindowRateLimiter.Hit ipHit = limiter.hit("ai-ip:" + clientIp, ipLimit);
        FixedWindowRateLimiter.Hit identityHit = limiter.hit("ai-user:" + resolveIdentity(clientIp), userLimit);

        if (identityHit.exceeded()) {
            return identityHit;
        }
        if (ipHit.exceeded()) {
            return ipHit;
        }
        return identityHit.remaining() <= ipHit.remaining() ? identityHit : ipHit;
    }

    /**
     * The authenticated username, or an IP-scoped anonymous identity. Bucketing anonymous callers
     * together would let a single visitor exhaust the public summarize endpoint for everyone.
     */
    private String resolveIdentity(String clientIp) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return "anonymous@" + clientIp;
        }
        return authentication.getName();
    }
}
