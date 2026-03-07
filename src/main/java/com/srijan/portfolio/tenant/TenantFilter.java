package com.srijan.portfolio.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String host = request.getHeader("Host");

        if (host != null) {

            String hostWithoutPort = host.split(":")[0];

            if (hostWithoutPort.endsWith(".lvh.me")) {

                String tenant =
                        hostWithoutPort.replace(".lvh.me", "");

                TenantContext.setTenant(tenant);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}