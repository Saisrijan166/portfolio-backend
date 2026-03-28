package com.srijan.portfolio.auth;

import com.srijan.portfolio.dto.AuthResponse;
import com.srijan.portfolio.dto.RefreshResponse;
import com.srijan.portfolio.dto.RegisterRequest;
import com.srijan.portfolio.email.EmailService;
import com.srijan.portfolio.entity.AuthProvider;
import com.srijan.portfolio.entity.AuthProviderType;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.entity.UserStatus;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.exception.ConflictException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.AuthProviderRepository;
import com.srijan.portfolio.repository.UserRepository;
import com.srijan.portfolio.security.JwtUtil;
import com.srijan.portfolio.service.AuthSupportService;
import com.srijan.portfolio.service.EmailOtpService;
import com.srijan.portfolio.service.RefreshTokenService;
import com.srijan.portfolio.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthOrchestratorService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    @Lazy
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailOtpService emailOtpService;
    private final EmailService emailService;
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
        ensureLocalProvider(saved);
        return issueAuthResponse(saved, deviceInfo, ipAddress, "Password");
    }

    @Transactional
    public AuthResponse loginWithPassword(String identifier, String password, String deviceInfo, String ipAddress) {
        String normalizedIdentifier = authSupportService.resolveIdentifier(identifier, identifier);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedIdentifier, password));

        User user = findByIdentifier(normalizedIdentifier);
        requireActiveUser(user);
        authSupportService.enforceTenantAlignment(user.getUsername());
        ensureLocalProvider(user);

        return issueAuthResponse(user, deviceInfo, ipAddress, "Password");
    }

    @Transactional
    public AuthResponse loginWithOtp(String email, String otp, String deviceInfo, String ipAddress) {
        String normalizedEmail = emailOtpService.verifyLoginOtp(email, otp);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> createPasswordlessUser(normalizedEmail, true, "otp-user"));

        requireActiveUser(user);
        authSupportService.enforceTenantAlignment(user.getUsername());
        ensureLocalProvider(user);

        return issueAuthResponse(user, deviceInfo, ipAddress, "Email OTP");
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

    @Transactional
    public AuthResponse issueOAuthAuthResponse(User user, String deviceInfo, String ipAddress, String loginMethod) {
        requireActiveUser(user);
        authSupportService.enforceTenantAlignment(user.getUsername());
        return issueAuthResponse(user, deviceInfo, ipAddress, loginMethod);
    }

    @Transactional
    public RefreshResponse refreshSession(String refreshTokenValue, String deviceInfo, String ipAddress) {
        RefreshTokenService.IssuedRefreshToken rotated = refreshTokenService.rotateRefreshToken(refreshTokenValue, deviceInfo, ipAddress);
        User user = rotated.refreshToken().getUser();
        requireActiveUser(user);

        return RefreshResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getUsername(), user.getRole(), user.getId(), resolveTenantKey(user)))
                .refreshToken(rotated.rawToken())
                .username(user.getUsername())
                .userId(user.getId())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .tenantKey(resolveTenantKey(user))
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

    @Transactional(readOnly = true)
    public List<String> getProviders(User user) {
        return authProviderRepository.findAllByUserId(user.getId()).stream()
                .map(provider -> provider.getProvider().name())
                .sorted()
                .toList();
    }

    private User linkOrCreateOAuthUser(
            AuthProviderType provider,
            String providerUserId,
            String email,
            String usernameCandidate,
            boolean emailVerified
    ) {
        String normalizedEmail = authSupportService.normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAUTH_EMAIL_REQUIRED", "OAuth account did not provide a usable email address");
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

        if (user == null) {
            user = User.builder()
                    .username(authSupportService.generateAvailableUsername(usernameCandidate, provider.name().toLowerCase()))
                    .email(normalizedEmail)
                    .passwordHash("")
                    .role("ROLE_USER")
                    .isEmailVerified(emailVerified)
                    .status(UserStatus.ACTIVE)
                    .build();
            user = userRepository.save(user);
        } else if (emailVerified && !user.isEmailVerified()) {
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
                .passwordHash("")
                .role("ROLE_USER")
                .isEmailVerified(emailVerified)
                .status(UserStatus.ACTIVE)
                .build();
        User saved = userRepository.save(user);
        authSupportService.enforceTenantAlignment(saved.getUsername());
        return saved;
    }

    private AuthResponse issueAuthResponse(User user, String deviceInfo, String ipAddress, String loginMethod) {
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issueRefreshToken(user, deviceInfo, ipAddress);
        AuthResponse response = AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getUsername(), user.getRole(), user.getId(), resolveTenantKey(user)))
                .refreshToken(refreshToken.rawToken())
                .username(user.getUsername())
                .userId(user.getId())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .tenantKey(resolveTenantKey(user))
                .build();
        sendLoginAlertSafely(user, loginMethod, deviceInfo, ipAddress);
        return response;
    }

    private void ensureLocalProvider(User user) {
        String localProviderId = localProviderId(user);
        authProviderRepository.findByUserAndProvider(user, AuthProviderType.LOCAL)
                .ifPresentOrElse(existing -> {
                    if (!localProviderId.equals(existing.getProviderUserId())) {
                        authProviderRepository.findByProviderAndProviderUserId(AuthProviderType.LOCAL, localProviderId)
                                .filter(conflict -> !conflict.getUser().getId().equals(user.getId()))
                                .ifPresent(conflict -> {
                                    throw new ConflictException("PROVIDER_ALREADY_LINKED", "Local sign-in is linked to a different account");
                                });
                        existing.setProviderUserId(localProviderId);
                        authProviderRepository.save(existing);
                    }
                }, () -> linkProvider(user, AuthProviderType.LOCAL, localProviderId));
    }

    private void linkProviderIfMissing(User user, AuthProviderType provider, String providerUserId) {
        authProviderRepository.findByUserAndProvider(user, provider)
                .ifPresentOrElse(existing -> {
                    if (!existing.getProviderUserId().equals(providerUserId)) {
                        throw new ConflictException("PROVIDER_ALREADY_LINKED", "Provider is linked to a different account");
                    }
                }, () -> linkProvider(user, provider, providerUserId));
    }

    private void linkProvider(User user, AuthProviderType provider, String providerUserId) {
        authProviderRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .filter(existing -> !existing.getUser().getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("PROVIDER_ALREADY_LINKED", "Provider is linked to a different account");
                });

        AuthProvider authProvider = AuthProvider.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .build();
        authProviderRepository.save(authProvider);
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

    private void sendLoginAlertSafely(User user, String method, String deviceInfo, String ipAddress) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        try {
            emailService.sendLoginAlert(user.getEmail(), method, ipAddress, deviceInfo, null);
        } catch (Exception ignored) {
        }
    }

    private String resolveTenantKey(User user) {
        String tenant = TenantContext.getTenant();
        return tenant != null && !tenant.isBlank() ? tenant : user.getUsername();
    }

    private String localProviderId(User user) {
        return "local:" + user.getId();
    }
}
