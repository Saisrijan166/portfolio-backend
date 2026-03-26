package com.srijan.portfolio.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${auth.oauth.frontend-failure-url:http://localhost:3000/login}")
    private String frontendFailureUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendFailureUrl)
                .queryParam("authError", exception.getMessage())
                .build(true)
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}
