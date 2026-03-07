package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.AuthRequest;
import com.srijan.portfolio.dto.AuthResponse;
import com.srijan.portfolio.dto.RegisterRequest;
import com.srijan.portfolio.dto.RefreshRequest;
import com.srijan.portfolio.dto.RefreshResponse;
import com.srijan.portfolio.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestBody RefreshRequest request) {

        String accessToken =
                authService.refreshAccessToken(request.getRefreshToken());

        return ResponseEntity.ok(
                RefreshResponse.builder()
                        .accessToken(accessToken)
                        .build()
        );
    }
}
