package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.ExperienceDto;
import com.srijan.portfolio.service.PortfolioService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin: Experience CRUD  (/api/me/experience)
 * Soft-delete on DELETE.
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/me/experience")
public class AdminExperienceController {

    private final PortfolioService portfolioService;

    private String username(Authentication auth) {
        return auth.getName();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExperienceDto>>> getAll(Authentication auth) {
        return ApiResponses.ok(portfolioService.getMyExperience(username(auth)), "Experience loaded");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExperienceDto>> create(
            Authentication auth,
            @Valid @RequestBody ExperienceDto dto) {
        return ApiResponses.created(portfolioService.createExperience(username(auth), dto), "Experience created");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExperienceDto>> update(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody ExperienceDto dto) {
        return ApiResponses.ok(portfolioService.updateExperience(username(auth), id, dto), "Experience updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication auth,
            @PathVariable Long id) {
        portfolioService.deleteExperience(username(auth), id);
        return ApiResponses.ok(null, "Experience deleted");
    }
}
