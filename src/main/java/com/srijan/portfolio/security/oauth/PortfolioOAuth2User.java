package com.srijan.portfolio.security.oauth;

import com.srijan.portfolio.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class PortfolioOAuth2User implements OAuth2User, OidcUser {

    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final User user;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public PortfolioOAuth2User(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey,
            User user
    ) {
        this(authorities, attributes, nameAttributeKey, user, null, null);
    }

    public PortfolioOAuth2User(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey,
            User user,
            OidcIdToken idToken,
            OidcUserInfo userInfo
    ) {
        this.authorities = authorities;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.user = user;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    @Override
    public String getName() {
        Object value = attributes.get(nameAttributeKey);
        return value != null ? value.toString() : user.getUsername();
    }

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }
}
