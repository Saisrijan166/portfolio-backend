package com.srijan.portfolio.security.oauth;

import com.srijan.portfolio.dto.AuthResponse;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.service.AuthService;
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

import java.security.SecureRandom;
import java.util.Base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectProvider<AuthService> authServiceProvider;

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
            writeOAuthCompletionPage(response, authResponse);
        } catch (Exception exception) {
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendFailureUrl)
                    .queryParam("authError", resolveErrorMessage(exception))
                    .build()
                    .encode()
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

    private String resolveErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? "OAuth authentication failed"
                : message;
    }

    private void writeOAuthCompletionPage(HttpServletResponse response, AuthResponse authResponse) throws IOException {
        String completionUrl = resolveFrontendCompletionUrl();
        String frontendOrigin = resolveFrontendOrigin();
        String nonce = generateCspNonce();

        // Override global CSP: allow ONLY the nonce'd script and form-action to the frontend
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; script-src 'nonce-" + nonce + "'; form-action " + frontendOrigin);
        response.setHeader("Referrer-Policy", "same-origin");

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <meta http-equiv="Cache-Control" content="no-store, no-cache, must-revalidate, max-age=0" />
                  <meta http-equiv="Pragma" content="no-cache" />
                  <meta http-equiv="Expires" content="0" />
                  <meta name="referrer" content="same-origin" />
                  <title>Completing sign-in</title>
                </head>
                <body>
                  <form id="oauth-complete" method="post" action="%s">
                    %s
                  </form>
                  <script nonce="%s">document.getElementById('oauth-complete').submit();</script>
                </body>
                </html>
                """.formatted(
                escapeHtml(completionUrl),
                buildHiddenInputs(authResponse),
                escapeHtml(nonce)
        ));
    }

    private String resolveFrontendCompletionUrl() {
        return UriComponentsBuilder.fromUriString(frontendSuccessUrl)
                .replacePath("/api/auth/oauth/complete")
                .replaceQuery(null)
                .build(true)
                .toUriString();
    }

    private String resolveFrontendOrigin() {
        return UriComponentsBuilder.fromUriString(frontendSuccessUrl)
                .replacePath(null)
                .replaceQuery(null)
                .build(true)
                .toUriString();
    }

    private static String generateCspNonce() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String buildHiddenInputs(AuthResponse authResponse) {
        StringBuilder html = new StringBuilder();
        appendHiddenInput(html, "accessToken", authResponse.getAccessToken());
        appendHiddenInput(html, "refreshToken", authResponse.getRefreshToken());
        appendHiddenInput(html, "username", authResponse.getUsername());
        appendHiddenInput(html, "userId", authResponse.getUserId() != null ? authResponse.getUserId().toString() : "");
        appendHiddenInput(html, "email", authResponse.getEmail());
        appendHiddenInput(html, "emailVerified", Boolean.toString(authResponse.isEmailVerified()));
        appendHiddenInput(html, "tenantKey", authResponse.getTenantKey());
        return html.toString();
    }

    private void appendHiddenInput(StringBuilder html, String name, String value) {
        html.append("<input type=\"hidden\" name=\"")
                .append(escapeHtml(name))
                .append("\" value=\"")
                .append(escapeHtml(value))
                .append("\" />");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
