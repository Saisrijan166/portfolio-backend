package com.srijan.portfolio.tenant;

import com.srijan.portfolio.util.EnvironmentUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final Pattern TENANT_PATTERN =
            Pattern.compile("^[a-z0-9][a-z0-9-]{2,30}$");

    private static final Set<String> RESERVED_TENANTS = Set.of(
            "admin",
            "api",
            "system",
            "dashboard",
            "login",
            "register",
            "_sites",
            "portfolio",
            "home",
            "www"
    );

    private static final List<String> ROOT_DOMAINS = parseRootDomains(
            EnvironmentUtils.get("TENANT_ROOT_DOMAINS")
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String host = request.getHeader("Host");
        String tenant = null;

        if (host != null) {

            String hostWithoutPort = host.split(":")[0].toLowerCase();

            tenant = resolveTenantFromHost(hostWithoutPort);
        }

        if (tenant == null) {
            tenant = resolveTenantFromPath(request);
        }

        if (tenant != null) {
            TenantContext.setTenant(tenant);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private static List<String> parseRootDomains(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of(
                    "localhost",
                    "portfoliooss.vercel.app",
                    "portfolioos-preprod.vercel.app"
            );
        }

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toLowerCase)
                .toList();
    }

    private static String resolveTenantFromHost(String host) {
        for (String rootDomain : ROOT_DOMAINS) {
            if (host.equals(rootDomain)) {
                return null;
            }

            if (host.endsWith("." + rootDomain)) {
                String subdomain = host.substring(0, host.length() - rootDomain.length() - 1);
                return isValidTenant(subdomain) ? subdomain : null;
            }
        }

        return null;
    }

    private static String resolveTenantFromPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (path.startsWith("/api/")) {
            return null;
        }

        String[] segments = path.split("/");
        for (String segment : segments) {
            if (segment == null || segment.isEmpty()) {
                continue;
            }

            String candidate = segment.toLowerCase();
            if (isValidTenant(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean isValidTenant(String candidate) {
        return candidate != null
                && TENANT_PATTERN.matcher(candidate).matches()
                && !RESERVED_TENANTS.contains(candidate);
    }
}
