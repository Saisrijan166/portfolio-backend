package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.ResumeDto;
import com.srijan.portfolio.service.PortfolioService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Admin: Resume management  (/api/me/resume)
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/me/resume")
public class AdminResumeController {

    private final PortfolioService portfolioService;

    private String username(Authentication auth) {
        return auth.getName();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResumeDto>> getResume(Authentication auth) {
        return ApiResponses.ok(portfolioService.getMyResume(username(auth)), "Resume loaded");
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ResumeDto>> updateResume(
            Authentication auth,
            @Valid @RequestBody ResumeDto dto) {
        return ApiResponses.ok(portfolioService.updateResume(username(auth), dto), "Resume updated");
    }
}
