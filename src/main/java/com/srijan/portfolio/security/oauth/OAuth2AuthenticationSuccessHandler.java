package com.srijan.portfolio.security.oauth;

import com.srijan.portfolio.dto.AuthResponse;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.service.AuthService;
import com.srijan.portfolio.service.RefreshTokenCookieService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectProvider<AuthService> authServiceProvider;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @Value("${auth.oauth.frontend-success-url:http://localhost:3000/auth/callback}")
    private String frontendSuccessUrl;

    @Value("${auth.oauth.frontend-failure-url:http://localhost:3000/login}")
    private String frontendFailureUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            PortfolioOAuth2User principal = (PortfolioOAuth2User) authentication.getPrincipal();
            User user = principal.getUser();
            String loginMethod = resolveLoginMethod(request.getRequestURI());
            AuthResponse authResponse = authServiceProvider.getObject()
                    .issueAuthResponse(user, request.getHeader("User-Agent"), request.getRemoteAddr(), loginMethod);
            refreshTokenCookieService.writeRefreshTokenCookie(response, authResponse.getRefreshToken());

            String redirectUrl = UriComponentsBuilder.fromUriString(frontendSuccessUrl)
                    .queryParam("oauth", "success")
                    .build(true)
                    .toUriString();

            response.sendRedirect(redirectUrl);
        } catch (Exception exception) {
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendFailureUrl)
                    .queryParam("authError", exception.getMessage())
                    .build(true)
                    .toUriString();
            response.sendRedirect(redirectUrl);
        }
    }

    private String resolveLoginMethod(String requestUri) {
        if (requestUri == null) {
            return "OAuth";
        }
        if (requestUri.contains("/google/")) {
            return "Google";
        }
        if (requestUri.contains("/github/")) {
            return "GitHub";
        }
        return "OAuth";
    }
}
