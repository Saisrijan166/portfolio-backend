package com.srijan.portfolio.security;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the standard quota headers so clients can back off instead of retrying blindly.
 * These names are listed in the CORS exposed-headers config, otherwise the browser hides them.
 */
final class RateLimitHeaders {

    private RateLimitHeaders() {
    }

    static void apply(HttpServletResponse response, FixedWindowRateLimiter.Hit hit) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(hit.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(hit.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(hit.resetEpochSecond()));
        if (hit.exceeded()) {
            response.setHeader("Retry-After", String.valueOf(hit.retryAfterSeconds()));
        }
    }
}
