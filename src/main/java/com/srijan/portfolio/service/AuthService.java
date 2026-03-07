package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.AuthRequest;
import com.srijan.portfolio.dto.AuthResponse;
import com.srijan.portfolio.dto.RegisterRequest;
import com.srijan.portfolio.entity.RefreshToken;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.repository.UserRepository;
import com.srijan.portfolio.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final AuthenticationManager authenticationManager;
        private final RefreshTokenService refreshTokenService;

        public AuthResponse register(RegisterRequest request) {

                if (userRepository.existsByUsername(request.getUsername())) {
                        throw new RuntimeException("Username is already taken!");
                }

                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email is already in use!");
                }

                User user = User.builder()
                                .username(request.getUsername())
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .role("ROLE_USER")
                                .build();

                userRepository.save(user);

                String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole());

                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken.getToken())
                                .username(user.getUsername())
                                .userId(user.getId())
                                .build();
        }

        public AuthResponse login(AuthRequest request) {

                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getUsername(),
                                                request.getPassword()));

                User user = userRepository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole());

                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken.getToken())
                                .username(user.getUsername())
                                .userId(user.getId())
                                .build();
        }

        public String refreshAccessToken(String refreshTokenValue) {

                RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenValue);

                refreshTokenService.verifyExpiration(refreshToken);

                User user = refreshToken.getUser();

                return jwtUtil.generateAccessToken(
                                user.getUsername(),
                                user.getRole());
        }
}