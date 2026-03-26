package com.srijan.portfolio.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenCookieService {

    @Value("${auth.refresh-cookie.name:refresh_token}")
    private String cookieName;

    @Value("${auth.refresh-cookie.secure:false}")
    private boolean secure;

    @Value("${auth.refresh-cookie.same-site:Lax}")
    private String sameSite;

    @Value("${auth.refresh-cookie.path:/}")
    private String path;

    @Value("${auth.refresh-cookie.max-age-seconds:604800}")
    private long maxAgeSeconds;

    public void writeRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAgeSeconds)
                .build()
                .toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0)
                .build()
                .toString());
    }

    public String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
