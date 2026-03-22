package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.AuthRequest;
import com.srijan.portfolio.dto.AuthResponse;
import com.srijan.portfolio.dto.RefreshResponse;
import com.srijan.portfolio.dto.RegisterRequest;
import com.srijan.portfolio.entity.RefreshToken;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ConflictException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.UserRepository;
import com.srijan.portfolio.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedUsername = normalizeUsername(request.getUsername());
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ConflictException("USERNAME_ALREADY_EXISTS", "Username is already taken");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "Email is already in use");
        }

        User user = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        return buildAuthResponse(user, refreshTokenService.rotateRefreshToken(user));
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        String normalizedUsername = normalizeUsername(request.getUsername());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedUsername, request.getPassword()));

        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(user, refreshTokenService.rotateRefreshToken(user));
    }

    @Transactional
    public RefreshResponse refreshSession(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.verifyExpiration(
                refreshTokenService.findByToken(refreshTokenValue)
        );

        User user = refreshToken.getUser();
        RefreshToken rotatedToken = refreshTokenService.rotateRefreshToken(user);

        return RefreshResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getUsername(), user.getRole()))
                .refreshToken(rotatedToken.getToken())
                .build();
    }

    @Transactional
    public void logoutSession(String refreshTokenValue) {
        refreshTokenService.revokeByToken(refreshTokenValue);
    }

    private AuthResponse buildAuthResponse(User user, RefreshToken refreshToken) {
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getUsername(), user.getRole()))
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
