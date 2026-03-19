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
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("USERNAME_ALREADY_EXISTS", "Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "Email is already in use");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        return buildAuthResponse(user, refreshTokenService.rotateRefreshToken(user));
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
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

    private AuthResponse buildAuthResponse(User user, RefreshToken refreshToken) {
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getUsername(), user.getRole()))
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }
}
