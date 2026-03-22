package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.AccountDto;
import com.srijan.portfolio.dto.ChangePasswordRequest;
import com.srijan.portfolio.dto.ChangeUsernameRequest;
import com.srijan.portfolio.dto.UsernameAvailabilityDto;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ConflictException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
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
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public AccountDto getMyAccount(String username) {
        return toDto(findUserByUsername(username));
    }

    @Transactional(readOnly = true)
    public UsernameAvailabilityDto checkUsernameAvailability(String currentUsername, String requestedUsername) {
        String normalized = normalizeUsername(requestedUsername);
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
        String normalized = normalizeUsername(request.getUsername());

        if (user.getUsername().equals(normalized)) {
            return toDto(user);
        }

        if (userRepository.existsByUsernameIgnoreCase(normalized)) {
            throw new ConflictException("USERNAME_ALREADY_EXISTS", "Username is already taken");
        }

        user.setUsername(normalized);

        try {
            User saved = userRepository.saveAndFlush(user);
            refreshTokenService.revokeAllForUser(saved.getId());
            return toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("USERNAME_ALREADY_EXISTS", "Username is already taken");
        }
    }

    @Transactional
    public void changePassword(String currentUsername, ChangePasswordRequest request) {
        User user = findUserByUsername(currentUsername);

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ConflictException("PASSWORD_UNCHANGED", "New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private AccountDto toDto(User user) {
        return AccountDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
