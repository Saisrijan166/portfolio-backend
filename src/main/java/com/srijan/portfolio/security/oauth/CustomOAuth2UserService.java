package com.srijan.portfolio.security.oauth;

import com.srijan.portfolio.entity.AuthProviderType;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.service.AuthService;
import com.srijan.portfolio.service.AuthSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AuthService authService;
    private final AuthSupportService authSupportService;
    private final RestClient restClient = RestClient.builder().build();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User delegate = new DefaultOAuth2UserService().loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = delegate.getAttributes();

        OAuthProfile profile = extractProfile(registrationId, attributes, userRequest.getAccessToken().getTokenValue());
        User user = authService.resolveOrCreateOAuthUser(
                profile.provider(),
                profile.providerUserId(),
                profile.email(),
                profile.usernameCandidate(),
                profile.emailVerified()
        );

        return new PortfolioOAuth2User(
                delegate.getAuthorities(),
                attributes,
                userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(),
                user
        );
    }

    private OAuthProfile extractProfile(String registrationId, Map<String, Object> attributes, String accessToken) {
        AuthProviderType provider = AuthProviderType.valueOf(registrationId.toUpperCase());

        return switch (provider) {
            case GOOGLE -> new OAuthProfile(
                    provider,
                    value(attributes, "sub"),
                    authSupportService.normalizeEmail(value(attributes, "email")),
                    firstNonBlank(value(attributes, "email"), value(attributes, "name"), value(attributes, "sub")),
                    Boolean.TRUE.equals(attributes.get("email_verified"))
            );
            case LINKEDIN -> new OAuthProfile(
                    provider,
                    value(attributes, "sub"),
                    authSupportService.normalizeEmail(value(attributes, "email")),
                    firstNonBlank(value(attributes, "name"), value(attributes, "email"), value(attributes, "sub")),
                    true
            );
            case GITHUB -> {
                String email = authSupportService.normalizeEmail(value(attributes, "email"));
                if (email == null) {
                    email = fetchGithubEmail(accessToken);
                }
                yield new OAuthProfile(
                        provider,
                        String.valueOf(attributes.get("id")),
                        email,
                        firstNonBlank(value(attributes, "login"), value(attributes, "name"), String.valueOf(attributes.get("id"))),
                        email != null
                );
            }
            default -> throw new OAuth2AuthenticationException("Unsupported OAuth provider");
        };
    }

    @SuppressWarnings("unchecked")
    private String fetchGithubEmail(String accessToken) {
        List<Map<String, Object>> emails = restClient.get()
                .uri("https://api.github.com/user/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(List.class);

        if (emails == null) {
            return null;
        }

        return emails.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.get("primary")) || Boolean.TRUE.equals(entry.get("verified")))
                .map(entry -> entry.get("email"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .map(authSupportService::normalizeEmail)
                .orElse(null);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String value(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? null : value.toString();
    }

    private record OAuthProfile(
            AuthProviderType provider,
            String providerUserId,
            String email,
            String usernameCandidate,
            boolean emailVerified
    ) {
    }
}
