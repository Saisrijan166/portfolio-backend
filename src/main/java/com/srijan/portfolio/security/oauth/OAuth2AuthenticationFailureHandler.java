package com.srijan.portfolio.security.oauth;

import com.srijan.portfolio.exception.UserFacingErrors;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
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
                .queryParam("authError", resolveErrorMessage(exception))
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    /**
     * Never forwards the raw exception text to the login page. When the database or an upstream is
     * down, that text is a driver diagnostic — it was being rendered verbatim in the sign-in form.
     */
    private String resolveErrorMessage(AuthenticationException exception) {
        log.warn("OAuth authentication failed", exception);
        UserFacingErrors.SafeError safeError = UserFacingErrors.resolve(exception);
        return "INTERNAL_SERVER_ERROR".equals(safeError.code())
                ? "We couldn't complete sign-in. Please try again."
                : safeError.message();
    }
}
