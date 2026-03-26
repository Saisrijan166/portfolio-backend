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
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @Value("${auth.oauth.frontend-success-url:http://localhost:3000/auth/callback}")
    private String frontendSuccessUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        PortfolioOAuth2User principal = (PortfolioOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();
        AuthResponse authResponse = authService.issueAuthResponse(user, request.getHeader("User-Agent"), request.getRemoteAddr());
        refreshTokenCookieService.writeRefreshTokenCookie(response, authResponse.getRefreshToken());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendSuccessUrl)
                .queryParam("accessToken", authResponse.getAccessToken())
                .queryParam("username", authResponse.getUsername())
                .queryParam("userId", authResponse.getUserId())
                .queryParam("email", authResponse.getEmail())
                .queryParam("emailVerified", authResponse.isEmailVerified())
                .build(true)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
