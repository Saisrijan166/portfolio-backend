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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${cron.api.key}")
    private String configuredApiKey;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        if (path != null && path.startsWith("/api/cron/")) {
            String apiKey = request.getHeader("X-API-Key");
            
            if (apiKey == null || configuredApiKey == null || !MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8), configuredApiKey.getBytes(StandardCharsets.UTF_8))) {
                log.warn("Unauthorized invocation of cron endpoint: {}", path);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                // Same envelope as every other error response, so callers parse one shape.
                objectMapper.writeValue(response.getWriter(), ApiResponses.error(
                        "UNAUTHORIZED",
                        "Authentication is required to access this resource"
                ));
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
