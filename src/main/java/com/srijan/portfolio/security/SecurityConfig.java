// src/main/java/com/srijan/portfolio/security/SecurityConfig.java
package com.srijan.portfolio.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.srijan.portfolio.tenant.TenantFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.cors.*;

import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final TenantFilter tenantFilter;
        private final UserDetailsService userDetailsService;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth

                                                // Preflight
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // Authentication
                                                .requestMatchers("/api/auth/**").permitAll()

                                                // Public portfolio viewing
                                                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                                                // Admin only APIs
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                                // Logged-in user APIs
                                                .requestMatchers("/api/me/**").authenticated()

                                                // Everything else secured
                                                .anyRequest().authenticated())

                                .authenticationProvider(authenticationProvider())

                                .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class)

                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                                .exceptionHandling(exceptionHandling -> exceptionHandling
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setContentType("application/json");
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.getWriter().write(
                                                                        "{\"error\": \"Unauthorized\", \"message\": \"Full authentication is required to access this resource\"}");
                                                }));

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {

                CorsConfiguration config = new CorsConfiguration();

                config.setAllowCredentials(true);

                config.setAllowedOriginPatterns(resolveAllowedOriginPatterns());

                config.setAllowedMethods(List.of(
                                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

                config.setAllowedHeaders(List.of("*"));

                config.setExposedHeaders(List.of("Authorization"));

                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration("/**", config);

                return source;
        }

        private List<String> resolveAllowedOriginPatterns() {
                String raw = System.getenv("CORS_ALLOWED_ORIGIN_PATTERNS");

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

                authProvider.setPasswordEncoder(passwordEncoder());

                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration config) throws Exception {

                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {

                return new BCryptPasswordEncoder();
        }
}
