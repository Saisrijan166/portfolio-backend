package com.srijan.portfolio.config;

import com.srijan.portfolio.security.ApiKeyAuthenticationFilter;
import com.srijan.portfolio.security.AiEndpointRateLimitingFilter;
import com.srijan.portfolio.security.ClientIpFilter;
import com.srijan.portfolio.security.JwtAuthenticationFilter;
import com.srijan.portfolio.security.RequestLoggingFilter;
import com.srijan.portfolio.security.RequestRateLimitingFilter;
import com.srijan.portfolio.security.SecurityHeaderFilter;
import com.srijan.portfolio.tenant.TenantFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Controls how the application's servlet filters are registered.
 *
 * <p>Spring Boot auto-registers every {@link Filter} bean into the servlet container's chain, which
 * runs <em>before</em> the Spring Security chain. Because all of these filters extend
 * {@code OncePerRequestFilter}, that auto-registered copy runs first and marks the request as
 * already filtered, turning the carefully ordered registrations in {@code SecurityConfig} into
 * no-ops. The practical consequences were that the AI rate limiter ran before authentication (so it
 * never saw a principal), and that the relative order of the filters was left to bean discovery
 * order. Disabling auto-registration here makes {@code SecurityConfig} the single place that
 * defines filter order.
 *
 * <p>{@link ClientIpFilter} is the one filter that must run outside the security chain: it has to
 * read {@code X-Forwarded-For} before Spring's {@code ForwardedHeaderFilter} strips it.
 */
@Configuration
public class FilterRegistrationConfig {

    /** Spring Boot registers ForwardedHeaderFilter at HIGHEST_PRECEDENCE + 30; stay ahead of it. */
    private static final int CLIENT_IP_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Bean
    public FilterRegistrationBean<ClientIpFilter> clientIpFilterRegistration(
            @Value("${security.trust-proxy-headers:false}") boolean trustProxyHeaders,
            @Value("${security.trusted-proxy-count:1}") int trustedProxyCount
    ) {
        FilterRegistrationBean<ClientIpFilter> registration =
                new FilterRegistrationBean<>(new ClientIpFilter(trustProxyHeaders, trustedProxyCount));
        registration.addUrlPatterns("/*");
        registration.setOrder(CLIENT_IP_FILTER_ORDER);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SecurityHeaderFilter> securityHeaderFilterRegistration(SecurityHeaderFilter filter) {
        return disableAutoRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<RequestRateLimitingFilter> requestRateLimitingFilterRegistration(
            RequestRateLimitingFilter filter) {
        return disableAutoRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<AiEndpointRateLimitingFilter> aiEndpointRateLimitingFilterRegistration(
            AiEndpointRateLimitingFilter filter) {
        return disableAutoRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilterRegistration(
            ApiKeyAuthenticationFilter filter) {
        return disableAutoRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration(RequestLoggingFilter filter) {
        return disableAutoRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter filter) {
        return disableAutoRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        return disableAutoRegistration(filter);
    }

    /**
     * Keeps the filter out of the servlet container chain. Referencing the bean from a
     * {@link FilterRegistrationBean} is also what stops Boot from auto-registering it separately, so
     * the filter runs exactly once, at the position {@code SecurityConfig} gives it.
     */
    private <T extends Filter> FilterRegistrationBean<T> disableAutoRegistration(T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
