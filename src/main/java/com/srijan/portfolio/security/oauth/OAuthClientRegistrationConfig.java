package com.srijan.portfolio.security.oauth;

import com.srijan.portfolio.util.EnvironmentUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Configuration
public class OAuthClientRegistrationConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        addIfConfigured(registrations, buildGoogle());
        addIfConfigured(registrations, buildGithub());

        if (registrations.isEmpty()) {
            return new EmptyClientRegistrationRepository();
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    private void addIfConfigured(List<ClientRegistration> registrations, ClientRegistration registration) {
        if (registration != null) {
            registrations.add(registration);
        }
    }

    private ClientRegistration buildGoogle() {
        String clientId = env("GOOGLE_CLIENT_ID");
        String clientSecret = env("GOOGLE_CLIENT_SECRET");
        if (clientId == null || clientSecret == null) {
            return null;
        }

        return CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri("{baseUrl}/api/auth/oauth/google/callback")
                .build();
    }

    private ClientRegistration buildGithub() {
        String clientId = env("GITHUB_CLIENT_ID");
        String clientSecret = env("GITHUB_CLIENT_SECRET");
        if (clientId == null || clientSecret == null) {
            return null;
        }

        return CommonOAuth2Provider.GITHUB.getBuilder("github")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri("{baseUrl}/api/auth/oauth/github/callback")
                .scope("read:user", "user:email")
                .build();
    }

    private String env(String key) {
        return EnvironmentUtils.get(key);
    }

    private static final class EmptyClientRegistrationRepository
            implements ClientRegistrationRepository, Iterable<ClientRegistration> {

        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return null;
        }

        @Override
        public Iterator<ClientRegistration> iterator() {
            return Collections.emptyIterator();
        }
    }
}
