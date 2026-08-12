package com.srijan.portfolio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Captures the real client IP and stashes it on the request as an attribute.
 *
 * <p>This filter is registered manually (see {@code FilterRegistrationConfig}) at an order that
 * places it <em>before</em> Spring's {@code ForwardedHeaderFilter}, which is active because
 * {@code server.forward-headers-strategy=framework}. That filter strips every {@code X-Forwarded-*}
 * header from the request and rewrites {@code getRemoteAddr()} to the <em>first</em> entry of
 * {@code X-Forwarded-For} — an entry the client fully controls. Anything downstream (rate limiting,
 * audit logging) therefore cannot recover a trustworthy IP on its own.
 *
 * <p>The trustworthy entry is counted from the <em>right</em>: each proxy appends the address it
 * saw, so with {@code trustedProxyCount=1} (one reverse proxy in front of the app, e.g. Render) the
 * last entry is the address that proxy observed. Entries to the left of it are client-supplied and
 * must never be trusted.
 */
public class ClientIpFilter extends OncePerRequestFilter {

    public static final String CLIENT_IP_ATTRIBUTE = "com.srijan.portfolio.CLIENT_IP";

    private static final String UNKNOWN_IP = "unknown";
    private static final int MAX_IP_LENGTH = 45;

    private final boolean trustProxyHeaders;
    private final int trustedProxyCount;

    public ClientIpFilter(boolean trustProxyHeaders, int trustedProxyCount) {
        this.trustProxyHeaders = trustProxyHeaders;
        this.trustedProxyCount = Math.max(trustedProxyCount, 1);
    }

    /**
     * Returns the client IP captured for this request, falling back to the socket address if this
     * filter did not run (for example on a dispatch it is not registered for).
     */
    public static String resolve(HttpServletRequest request) {
        Object captured = request.getAttribute(CLIENT_IP_ATTRIBUTE);
        if (captured instanceof String value && !value.isBlank()) {
            return value;
        }
        return sanitize(request.getRemoteAddr(), UNKNOWN_IP);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        request.setAttribute(CLIENT_IP_ATTRIBUTE, resolveClientIp(request));
        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustProxyHeaders) {
            return sanitize(remoteAddr, UNKNOWN_IP);
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return sanitize(remoteAddr, UNKNOWN_IP);
        }

        String[] hops = forwardedFor.split(",");
        int index = Math.max(hops.length - trustedProxyCount, 0);
        return sanitize(hops[index], sanitize(remoteAddr, UNKNOWN_IP));
    }

    /**
     * Normalises a forwarded-for entry and rejects anything that is not plausibly an IP address.
     * Without this, a crafted header would let a caller mint unlimited distinct rate-limit keys.
     */
    private static String sanitize(String candidate, String fallback) {
        if (candidate == null) {
            return fallback;
        }

        String value = candidate.trim();
        if (value.startsWith("[")) {
            int closing = value.indexOf(']');
            value = closing > 0 ? value.substring(1, closing) : value.substring(1);
        } else {
            int colon = value.indexOf(':');
            // "1.2.3.4:5678" — an IPv4 address with a port. Bare IPv6 has more than one colon.
            if (colon > 0 && value.indexOf(':', colon + 1) < 0 && value.indexOf('.') >= 0) {
                value = value.substring(0, colon);
            }
        }

        if (value.isEmpty() || value.length() > MAX_IP_LENGTH || !isIpCharacters(value)) {
            return fallback;
        }
        return value;
    }

    private static boolean isIpCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean allowed = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F')
                    || c == '.'
                    || c == ':'
                    || c == '%';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }
}
