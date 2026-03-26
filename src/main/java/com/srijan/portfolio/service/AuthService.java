package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.AuthRequest;
import com.srijan.portfolio.dto.AuthResponse;
import com.srijan.portfolio.dto.RefreshResponse;
import com.srijan.portfolio.dto.RegisterRequest;
import com.srijan.portfolio.entity.AuthProvider;
import com.srijan.portfolio.entity.AuthProviderType;
import com.srijan.portfolio.entity.RefreshToken;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.entity.UserStatus;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.exception.ConflictException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.AuthProviderRepository;
import com.srijan.portfolio.repository.UserRepository;
import com.srijan.portfolio.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailOtpService emailOtpService;
    private final AuthSupportService authSupportService;

    @Transactional
    public AuthResponse register(RegisterRequest request, String deviceInfo, String ipAddress) {
        String normalizedUsername = authSupportService.normalizeUsername(request.getUsername());
        String normalizedEmail = authSupportService.normalizeEmail(request.getEmail());

        authSupportService.enforceTenantAlignment(normalizedUsername);

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ConflictException("USERNAME_ALREADY_EXISTS", "Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "Email is already in use");
        }

        User user = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .isEmailVerified(false)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        linkProvider(saved, AuthProviderType.LOCAL, normalizedUsername);
        return buildAuthResponse(saved, refreshTokenService.issueRefreshToken(saved, deviceInfo, ipAddress));
    }

    @Transactional
    public AuthResponse login(AuthRequest request, String deviceInfo, String ipAddress) {
        String normalizedIdentifier = authSupportService.resolveIdentifier(request.getUsername(), request.getIdentifier());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedIdentifier, request.getPassword()));

        User user = findByIdentifier(normalizedIdentifier);
        requireActiveUser(user);
        authSupportService.enforceTenantAlignment(user.getUsername());

        return buildAuthResponse(user, refreshTokenService.issueRefreshToken(user, deviceInfo, ipAddress));
    }

    @Transactional
    public AuthResponse loginWithOtp(String email, String otp, String deviceInfo, String ipAddress) {
        String normalizedEmail = emailOtpService.verifyLoginOtp(email, otp);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> createPasswordlessUser(normalizedEmail, true, "otp-user"));

        requireActiveUser(user);
        authSupportService.enforceTenantAlignment(user.getUsername());
        linkProviderIfMissing(user, AuthProviderType.LOCAL, user.getUsername());

        return buildAuthResponse(user, refreshTokenService.issueRefreshToken(user, deviceInfo, ipAddress));
    }

    @Transactional
    public RefreshResponse refreshSession(String refreshTokenValue, String deviceInfo, String ipAddress) {
        RefreshTokenService.IssuedRefreshToken rotated = refreshTokenService.rotateRefreshToken(refreshTokenValue, deviceInfo, ipAddress);
        User user = rotated.refreshToken().getUser();
        requireActiveUser(user);

        return RefreshResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getUsername(), user.getRole()))
                .refreshToken(rotated.rawToken())
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    @Transactional
    public void logoutSession(String refreshTokenValue) {
        refreshTokenService.revokeByToken(refreshTokenValue);
    }

    @Transactional
    public void logoutAllSessions(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public User resolveOrCreateOAuthUser(
            AuthProviderType provider,
            String providerUserId,
            String email,
            String usernameCandidate,
            boolean emailVerified
    ) {
        return authProviderRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(AuthProvider::getUser)
                .map(this::requireActiveUser)
                .orElseGet(() -> linkOrCreateOAuthUser(provider, providerUserId, email, usernameCandidate, emailVerified));
    }

    public AuthResponse issueAuthResponse(User user, String deviceInfo, String ipAddress) {
        requireActiveUser(user);
        authSupportService.enforceTenantAlignment(user.getUsername());
        return buildAuthResponse(user, refreshTokenService.issueRefreshToken(user, deviceInfo, ipAddress));
    }

    private User linkOrCreateOAuthUser(
            AuthProviderType provider,
            String providerUserId,
            String email,
            String usernameCandidate,
            boolean emailVerified
    ) {
        String normalizedEmail = authSupportService.normalizeEmail(email);
        User user = normalizedEmail == null
                ? null
                : userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

        if (user == null) {
            user = User.builder()
                    .username(authSupportService.generateAvailableUsername(usernameCandidate, provider.name().toLowerCase()))
                    .email(normalizedEmail)
                    .passwordHash(null)
                    .role("ROLE_USER")
                    .isEmailVerified(emailVerified && normalizedEmail != null)
                    .status(UserStatus.ACTIVE)
                    .build();
            user = userRepository.save(user);
        } else if (normalizedEmail != null && emailVerified && !user.isEmailVerified()) {
            user.setEmailVerified(true);
        }

        requireActiveUser(user);
        authSupportService.enforceTenantAlignment(user.getUsername());
        linkProviderIfMissing(user, provider, providerUserId);
        return user;
    }

    private User createPasswordlessUser(String email, boolean emailVerified, String fallbackPrefix) {
        String username = authSupportService.generateAvailableUsername(email != null ? email.substring(0, email.indexOf('@')) : fallbackPrefix, fallbackPrefix);
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(null)
                .role("ROLE_USER")
                .isEmailVerified(emailVerified)
                .status(UserStatus.ACTIVE)
                .build();
        User saved = userRepository.save(user);
        authSupportService.enforceTenantAlignment(saved.getUsername());
        return saved;
    }

    private void linkProvider(User user, AuthProviderType provider, String providerUserId) {
        AuthProvider authProvider = AuthProvider.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .build();
        authProviderRepository.save(authProvider);
    }

    private void linkProviderIfMissing(User user, AuthProviderType provider, String providerUserId) {
        authProviderRepository.findByUserAndProvider(user, provider)
                .ifPresentOrElse(existing -> {
                    if (!existing.getProviderUserId().equals(providerUserId)) {
                        throw new ConflictException("PROVIDER_ALREADY_LINKED", "Provider is linked to a different account");
                    }
                }, () -> linkProvider(user, provider, providerUserId));
    }

    private User findByIdentifier(String identifier) {
        return authSupportService.isEmailIdentifier(identifier)
                ? userRepository.findByEmailIgnoreCase(identifier).orElseThrow(() -> new ResourceNotFoundException("User not found"))
                : userRepository.findByUsernameIgnoreCase(identifier).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User requireActiveUser(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "Account is suspended");
        }
        return user;
    }

    private AuthResponse buildAuthResponse(User user, RefreshTokenService.IssuedRefreshToken refreshToken) {
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getUsername(), user.getRole()))
                .refreshToken(refreshToken.rawToken())
                .username(user.getUsername())
                .userId(user.getId())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    public List<String> getProviders(User user) {
        return authProviderRepository.findAllByUserId(user.getId()).stream()
                .map(provider -> provider.getProvider().name())
                .sorted()
                .toList();
    }
}
