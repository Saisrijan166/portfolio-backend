package com.srijan.portfolio.security;

import com.srijan.portfolio.tenant.TenantFilter;
import com.srijan.portfolio.security.oauth.CustomOAuth2UserService;
import com.srijan.portfolio.security.oauth.CustomOidcUserService;
import com.srijan.portfolio.security.oauth.OAuth2AuthenticationFailureHandler;
import com.srijan.portfolio.security.oauth.OAuth2AuthenticationSuccessHandler;
import com.srijan.portfolio.util.EnvironmentUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final TenantFilter tenantFilter;
    private final RequestRateLimitingFilter requestRateLimitingFilter;
    private final AiEndpointRateLimitingFilter aiEndpointRateLimitingFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final SecurityHeaderFilter securityHeaderFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .frameOptions(frame -> frame.sameOrigin())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // The container's error dispatch is filtered like any other request. Without
                        // this, an error raised for an anonymous caller is answered with 403 instead
                        // of the real status.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/users/count", "/api/users/health").permitAll()
                        .requestMatchers("/api/auth/logout-all").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/ai/summarize-section",
                                "/api/public/portfolio/*/feedback",
                                "/api/public/portfolio/*/feedback/platform",
                                "/api/public/portfolio/*/contact/message"
                        ).permitAll()
                        .requestMatchers("/api/cron/weekly-email").permitAll()
                        .requestMatchers("/api/admin/profile/**", "/api/admin/about/**").authenticated()
                        .requestMatchers("/api/superadmin/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/me/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth"))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth/*/callback"))
                        .userInfoEndpoint(endpoint -> endpoint
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler(oauth2AuthenticationSuccessHandler)
                        .failureHandler(oauth2AuthenticationFailureHandler)
                )
                .authenticationProvider(authenticationProvider())
                // Filter order. Everything below runs after Spring Security's CorsFilter, so 429 and
                // 403 responses carry CORS headers and the browser can actually read them, and before
                // the OAuth2 login filters, so the tenant context is set for the OAuth success handler.
                // Filters anchored to the same reference class keep their registration order.
                // Auto-registration into the servlet container chain is disabled in
                // FilterRegistrationConfig, so this is the only place that decides ordering.
                .addFilterBefore(requestLoggingFilter, CsrfFilter.class)
                // Rate limiting sits ahead of the API key and CSRF-header checks so that rejected
                // requests — cron key guessing included — are still counted against the caller.
                .addFilterBefore(requestRateLimitingFilter, CsrfFilter.class)
                .addFilterBefore(securityHeaderFilter, CsrfFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, CsrfFilter.class)
                .addFilterBefore(tenantFilter, CsrfFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Must stay after JWT authentication so the per-user AI budget sees the principal.
                .addFilterAfter(aiEndpointRateLimitingFilter, JwtAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(resolveAllowedOriginPatterns());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-Visitor-Token", "X-API-Key"));
        config.setExposedHeaders(List.of(
                "Authorization",
                "Retry-After",
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset"
        ));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> resolveAllowedOriginPatterns() {
        String raw = EnvironmentUtils.get("CORS_ALLOWED_ORIGIN_PATTERNS");
        if (raw == null || raw.isBlank()) {
            return List.of(
                    "http://localhost:*",
                    "http://*.localhost:*",
                    "https://portfolioos-preprod.vercel.app",
                    "https://*.portfolioos-preprod.vercel.app",
                    "https://portfoliooss.vercel.app",
                    "https://*.portfoliooss.vercel.app"
            );
        }

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
