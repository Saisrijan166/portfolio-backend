package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.EducationDto;
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
 * Admin: Education CRUD  (/api/me/education)
 * Soft-delete on DELETE.
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/me/education")
public class AdminEducationController {

    private final PortfolioService portfolioService;

    private String username(Authentication auth) {
        return auth.getName();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EducationDto>>> getAll(Authentication auth) {
        return ApiResponses.ok(portfolioService.getMyEducation(username(auth)), "Education loaded");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EducationDto>> create(
            Authentication auth,
            @Valid @RequestBody EducationDto dto) {
        return ApiResponses.created(portfolioService.createEducation(username(auth), dto), "Education created");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EducationDto>> update(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody EducationDto dto) {
        return ApiResponses.ok(portfolioService.updateEducation(username(auth), id, dto), "Education updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication auth,
            @PathVariable Long id) {
        portfolioService.deleteEducation(username(auth), id);
        return ApiResponses.ok(null, "Education deleted");
    }
}
