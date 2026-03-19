package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.ProjectDto;
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
 * Admin: Projects CRUD  (/api/me/projects)
 * Soft-delete on DELETE.
 * Ownership validated server-side via JWT username.
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/me/projects")
public class AdminProjectController {

    private final PortfolioService portfolioService;

    private String username(Authentication auth) {
        return auth.getName();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getAll(Authentication auth) {
        return ApiResponses.ok(portfolioService.getMyProjects(username(auth)), "Projects loaded");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDto>> create(
            Authentication auth,
            @Valid @RequestBody ProjectDto dto) {
        return ApiResponses.created(portfolioService.createProject(username(auth), dto), "Project created");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDto>> update(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody ProjectDto dto) {
        return ApiResponses.ok(portfolioService.updateProject(username(auth), id, dto), "Project updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication auth,
            @PathVariable Long id) {
        portfolioService.deleteProject(username(auth), id);
        return ApiResponses.ok(null, "Project deleted");
    }
}
