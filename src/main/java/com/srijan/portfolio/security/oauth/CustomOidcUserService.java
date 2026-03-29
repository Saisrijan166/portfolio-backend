package com.srijan.portfolio.security.oauth;

import com.srijan.portfolio.entity.AuthProviderType;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.service.AuthService;
import com.srijan.portfolio.service.AuthSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final ObjectProvider<AuthService> authServiceProvider;
    private final AuthSupportService authSupportService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OidcUser delegate = super.loadUser(userRequest);
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            AuthProviderType provider = AuthProviderType.valueOf(registrationId.toUpperCase());

            User user = authServiceProvider.getObject().resolveOrCreateOAuthUser(
                    provider,
                    requiredValue(delegate.getSubject(), "sub", registrationId),
                    authSupportService.normalizeEmail(delegate.getEmail()),
                    firstNonBlank(delegate.getEmail(), delegate.getFullName(), delegate.getSubject()),
                    Boolean.TRUE.equals(delegate.getEmailVerified())
            );

            return new PortfolioOAuth2User(
                    delegate.getAuthorities(),
                    delegate.getClaims(),
                    userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(),
                    user,
                    delegate.getIdToken(),
                    delegate.getUserInfo()
            );
        } catch (OAuth2AuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("oauth_user_resolution_failed"),
                    exception.getMessage(),
                    exception
            );
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String requiredValue(String value, String field, String providerName) {
        if (value == null || value.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("oauth_profile_incomplete"),
                    providerName + " account did not provide required " + field + " information"
            );
        }
        return value;
    }
}
