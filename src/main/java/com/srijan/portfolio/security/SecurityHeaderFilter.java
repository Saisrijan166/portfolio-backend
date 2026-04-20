package com.srijan.portfolio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.ApiResponse;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter to provide global CSRF defense for cross-site cookie usage (SameSite=None).
 * Enforces X-Requested-With header and validates Origin for all state-changing requests.
 */
@Component
@RequiredArgsConstructor
public class SecurityHeaderFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Value("${CORS_ALLOWED_ORIGIN_PATTERNS:}")
    private String allowedOrigins;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Skip validation for "Safe" methods (GET, HEAD, etc.) as they shouldn't change state
        if (SAFE_METHODS.contains(method.toUpperCase())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Skip validation for entirely public API branch (except Auth)
        // Note: Refresh is in /api/auth/ and MUST be protected.
        if (path.startsWith("/api/public/") || 
            path.startsWith("/api/cron/") ||
            path.equals("/api/auth/login") || 
            path.equals("/api/auth/register") ||
            path.startsWith("/api/auth/otp/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Enforce X-Requested-With header (standard defense against CSRF from simple requests)
        String requestedWith = request.getHeader("X-Requested-With");
        if (requestedWith == null || requestedWith.isBlank()) {
            writeErrorResponse(response, "MISSING_SECURITY_HEADER", "Security header (X-Requested-With) is missing");
            return;
        }

        // 4. Verify Origin (protection against CSRF and cross-site execution)
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank() && !isAllowedOrigin(origin)) {
            writeErrorResponse(response, "INVALID_ORIGIN", "Request origin is not allowed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedOrigin(String origin) {
        List<String> allowed = getParsedAllowedOrigins();
        return allowed.stream().anyMatch(pattern -> {
            if (pattern.contains("*")) {
                return origin.matches(pattern.replace(".", "\\.").replace("*", ".*"));
            }
            return origin.equalsIgnoreCase(pattern);
        });
    }

    private List<String> getParsedAllowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of(
                    "http://localhost:3000",
                    "https://portfoliooss.vercel.app",
                    "https://portfolioos-preprod.vercel.app"
            );
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private void writeErrorResponse(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponses.error(code, message));
    }
}
