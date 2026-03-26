package com.srijan.portfolio.security.oauth;

import com.srijan.portfolio.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class PortfolioOAuth2User implements OAuth2User {

    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final User user;

    public PortfolioOAuth2User(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey,
            User user
    ) {
        this.authorities = authorities;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.user = user;
    }

    @Override
    public String getName() {
        Object value = attributes.get(nameAttributeKey);
        return value != null ? value.toString() : user.getUsername();
    }
}
