package com.srijan.portfolio.service;

import com.srijan.portfolio.auth.AuthOrchestratorService;
import com.srijan.portfolio.dto.AuthRequest;
import com.srijan.portfolio.dto.AuthResponse;
import com.srijan.portfolio.dto.RefreshResponse;
import com.srijan.portfolio.dto.RegisterRequest;
import com.srijan.portfolio.entity.AuthProviderType;
import com.srijan.portfolio.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthOrchestratorService orchestrator;

    public AuthResponse register(RegisterRequest request, String deviceInfo, String ipAddress) {
        return orchestrator.register(request, deviceInfo, ipAddress);
    }

    public AuthResponse login(AuthRequest request, String deviceInfo, String ipAddress) {
        return orchestrator.loginWithPassword(
                request.getIdentifier() != null && !request.getIdentifier().isBlank() ? request.getIdentifier() : request.getUsername(),
                request.getPassword(),
                deviceInfo,
                ipAddress
        );
    }

    public AuthResponse loginWithOtp(String email, String otp, String deviceInfo, String ipAddress) {
        return orchestrator.loginWithOtp(email, otp, deviceInfo, ipAddress);
    }

    public RefreshResponse refreshSession(String refreshTokenValue, String deviceInfo, String ipAddress) {
        return orchestrator.refreshSession(refreshTokenValue, deviceInfo, ipAddress);
    }

    public void logoutSession(String refreshTokenValue) {
        orchestrator.logoutSession(refreshTokenValue);
    }

    public void logoutAllSessions(String username) {
        orchestrator.logoutAllSessions(username);
    }

    public User resolveOrCreateOAuthUser(
            AuthProviderType provider,
            String providerUserId,
            String email,
            String usernameCandidate,
            boolean emailVerified
    ) {
        return orchestrator.resolveOrCreateOAuthUser(provider, providerUserId, email, usernameCandidate, emailVerified);
    }

    public AuthResponse issueAuthResponse(User user, String deviceInfo, String ipAddress, String loginMethod) {
        return orchestrator.issueOAuthAuthResponse(user, deviceInfo, ipAddress, loginMethod);
    }

    public List<String> getProviders(User user) {
        return orchestrator.getProviders(user);
    }
}
