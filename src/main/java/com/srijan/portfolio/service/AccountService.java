package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.AccountDto;
import com.srijan.portfolio.dto.ChangePasswordRequest;
import com.srijan.portfolio.dto.ChangeUsernameRequest;
import com.srijan.portfolio.dto.UsernameAvailabilityDto;
import com.srijan.portfolio.email.EmailService;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ConflictException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.entity.AuthProviderType;
import com.srijan.portfolio.repository.AuthProviderRepository;
import com.srijan.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final AuthSupportService authSupportService;
    private final com.srijan.portfolio.email.TenantBrandingResolver tenantBrandingResolver;

    @Transactional(readOnly = true)
    public AccountDto getMyAccount(String username) {
        return toDto(findUserByUsername(username));
    }

    @Transactional(readOnly = true)
    public UsernameAvailabilityDto checkUsernameAvailability(String currentUsername, String requestedUsername) {
        String normalized = authSupportService.normalizeUsername(requestedUsername);
        boolean available = normalized.equalsIgnoreCase(currentUsername)
                || !userRepository.existsByUsernameIgnoreCase(normalized);

        return UsernameAvailabilityDto.builder()
                .username(normalized)
                .available(available)
                .build();
    }

    @Transactional
    public AccountDto changeUsername(String currentUsername, ChangeUsernameRequest request) {
        User user = findUserByUsername(currentUsername);
        String previousUsername = user.getUsername();
        String normalized = authSupportService.normalizeUsername(request.getUsername());

        if (user.getUsername().equals(normalized)) {
            return toDto(user);
        }

        if (userRepository.existsByUsernameIgnoreCase(normalized)) {
            throw new ConflictException("USERNAME_ALREADY_EXISTS", "Username is already taken");
        }

        user.setUsername(normalized);

        try {
            User saved = userRepository.saveAndFlush(user);
            authProviderRepository.findByUserAndProvider(saved, AuthProviderType.LOCAL)
                    .ifPresent(provider -> {
                        String expectedProviderId = "local:" + saved.getId();
                        if (!expectedProviderId.equals(provider.getProviderUserId())) {
                            provider.setProviderUserId(expectedProviderId);
                            authProviderRepository.save(provider);
                        }
                    });
            refreshTokenService.revokeAllForUser(saved.getId());
            sendUsernameChangedEmail(saved, previousUsername, normalized);
            return toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("USERNAME_ALREADY_EXISTS", "Username is already taken");
        }
    }

    @Transactional
    public AccountDto changePassword(String currentUsername, ChangePasswordRequest request) {
        User user = findUserByUsername(currentUsername);

        if (user.getPasswordHash() != null
                && !user.getPasswordHash().isBlank()
                && passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ConflictException("PASSWORD_UNCHANGED", "New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        User saved = userRepository.save(user);
        sendPasswordChangedEmail(saved);
        return toDto(saved);
    }

    @Transactional
    public AccountDto markEmailVerified(String currentUsername) {
        User user = findUserByUsername(currentUsername);
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ConflictException("EMAIL_REQUIRED", "An email address is required before email verification can be updated");
        }

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user = userRepository.save(user);
        }

        return toDto(user);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private AccountDto toDto(User user) {
        return AccountDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .status(user.getStatus().name())
                .hasPassword(user.getPasswordHash() != null && !user.getPasswordHash().isBlank())
                .providers(authProviderRepository.findAllByUserId(user.getId()).stream()
                        .map(provider -> provider.getProvider().name())
                        .sorted()
                        .toList())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void sendPasswordChangedEmail(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        try {
            com.srijan.portfolio.email.TenantEmailContext context = tenantBrandingResolver.resolve(user.getUsername());
            emailService.sendPasswordChangedEmail(user.getEmail(), false, context);
        } catch (Exception ignored) {
        }
    }

    private void sendUsernameChangedEmail(User user, String oldUsername, String newUsername) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        try {
            com.srijan.portfolio.email.TenantEmailContext context = tenantBrandingResolver.resolve(user.getUsername());
            emailService.sendUsernameChangedEmail(user.getEmail(), oldUsername, newUsername, context);
        } catch (Exception ignored) {
        }
    }
}
