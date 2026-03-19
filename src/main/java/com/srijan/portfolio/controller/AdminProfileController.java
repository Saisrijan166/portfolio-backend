package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.ProfileDto;
import com.srijan.portfolio.service.PortfolioService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Admin: Profile management  (/api/me/profile)
 * JWT required — userId resolved from authentication token.
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class AdminProfileController {

    private final PortfolioService portfolioService;

    private String username(Authentication auth) {
        return auth.getName();
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileDto>> getProfile(Authentication auth) {
        return ApiResponses.ok(portfolioService.getMyProfile(username(auth)), "Profile loaded");
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileDto>> updateProfile(
            Authentication auth,
            @Valid @RequestBody ProfileDto dto) {
        return ApiResponses.ok(portfolioService.updateProfile(username(auth), dto), "Profile updated");
    }
}
