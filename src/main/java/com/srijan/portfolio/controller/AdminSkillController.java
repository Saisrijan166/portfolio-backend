package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.SkillDto;
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
 * Admin: Skills CRUD  (/api/me/skills)
 * Hard delete on DELETE (skills have no soft-delete column).
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/me/skills")
public class AdminSkillController {

    private final PortfolioService portfolioService;

    private String username(Authentication auth) {
        return auth.getName();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillDto>>> getAll(Authentication auth) {
        return ApiResponses.ok(portfolioService.getMySkills(username(auth)), "Skills loaded");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SkillDto>> create(
            Authentication auth,
            @Valid @RequestBody SkillDto dto) {
        return ApiResponses.created(portfolioService.createSkill(username(auth), dto), "Skill created");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillDto>> update(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody SkillDto dto) {
        return ApiResponses.ok(portfolioService.updateSkill(username(auth), id, dto), "Skill updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication auth,
            @PathVariable Long id) {
        portfolioService.deleteSkill(username(auth), id);
        return ApiResponses.ok(null, "Skill deleted");
    }
}
