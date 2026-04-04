package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.CertificationAchievementDto;
import com.srijan.portfolio.service.PortfolioService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/me/certifications-achievements")
public class AdminCertificationAchievementController {

    private final PortfolioService portfolioService;

    private String username(Authentication auth) {
        return auth.getName();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CertificationAchievementDto>>> getAll(Authentication auth) {
        return ApiResponses.ok(
                portfolioService.getMyCertificationAchievements(username(auth)),
                "Certifications and achievements loaded"
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CertificationAchievementDto>> create(
            Authentication auth,
            @Valid @RequestBody CertificationAchievementDto dto) {
        return ApiResponses.created(
                portfolioService.createCertificationAchievement(username(auth), dto),
                "Certification or achievement created"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CertificationAchievementDto>> update(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody CertificationAchievementDto dto) {
        return ApiResponses.ok(
                portfolioService.updateCertificationAchievement(username(auth), id, dto),
                "Certification or achievement updated"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication auth,
            @PathVariable Long id) {
        portfolioService.deleteCertificationAchievement(username(auth), id);
        return ApiResponses.ok(null, "Certification or achievement deleted");
    }
}
