package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.AuthRequest;
import com.srijan.portfolio.dto.AuthResponse;
import com.srijan.portfolio.dto.OtpRequestDto;
import com.srijan.portfolio.dto.OtpVerifyRequestDto;
import com.srijan.portfolio.dto.RefreshRequest;
import com.srijan.portfolio.dto.RefreshResponse;
import com.srijan.portfolio.dto.RegisterRequest;
import com.srijan.portfolio.service.AuthService;
import com.srijan.portfolio.service.EmailOtpService;
import com.srijan.portfolio.service.RefreshTokenCookieService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailOtpService emailOtpService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponse response = authService.register(request, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr());
        refreshTokenCookieService.writeRefreshTokenCookie(httpResponse, response.getRefreshToken());
        return ApiResponses.ok(response, "Registration successful");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponse response = authService.login(request, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr());
        refreshTokenCookieService.writeRefreshTokenCookie(httpResponse, response.getRefreshToken());
        return ApiResponses.ok(response, "Login successful");
    }

    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@Valid @RequestBody OtpRequestDto request) {
        emailOtpService.sendLoginOtp(request);
        return ApiResponses.ok(null, "OTP sent successfully");
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponse response = authService.loginWithOtp(
                request.getEmail(),
                request.getOtp(),
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr()
        );
        refreshTokenCookieService.writeRefreshTokenCookie(httpResponse, response.getRefreshToken());
        return ApiResponses.ok(response, "OTP verified");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refreshToken(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = resolveRefreshToken(request, httpRequest);
        RefreshResponse response = authService.refreshSession(
                refreshToken,
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr()
        );
        refreshTokenCookieService.writeRefreshTokenCookie(httpResponse, response.getRefreshToken());
        return ApiResponses.ok(response, "Token refreshed");
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        authService.logoutSession(resolveRefreshToken(request, httpRequest));
        refreshTokenCookieService.clearRefreshTokenCookie(httpResponse);
        return ApiResponses.ok(null, "Logout successful");
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            Authentication authentication,
            HttpServletResponse httpResponse) {
        authService.logoutAllSessions(authentication.getName());
        refreshTokenCookieService.clearRefreshTokenCookie(httpResponse);
        return ApiResponses.ok(null, "Logged out from all devices");
    }

    private String resolveRefreshToken(RefreshRequest request, HttpServletRequest httpRequest) {
        String bodyToken = request != null ? request.getRefreshToken() : null;
        if (bodyToken != null && !bodyToken.isBlank()) {
            return bodyToken;
        }
        return refreshTokenCookieService.extractRefreshToken(httpRequest);
    }
}
