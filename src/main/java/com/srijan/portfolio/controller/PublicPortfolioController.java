package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.service.PortfolioService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
public class PublicPortfolioController {

    private static final String CACHE_PUBLIC = "no-store, max-age=0";
    private static final String USERNAME_PATTERN = "^[a-z0-9][a-z0-9-]{2,30}$";

    private final PortfolioService portfolioService;

    private <T> ResponseEntity<ApiResponse<T>> cachedSuccess(T data, String message) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_PUBLIC)
                .body(ApiResponses.success(data, message));
    }

    private <T> ResponseEntity<ApiResponse<T>> cachedUserNotFound() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_PUBLIC)
                .body(ApiResponses.success(null, "User not found"));
    }

    @GetMapping("/api/public/portfolio/{username}")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolioBootstrap(
            @PathVariable
            @Pattern(regexp = USERNAME_PATTERN, message = "Username format is invalid")
            String username) {
        PortfolioResponse data = portfolioService.getPortfolioBootstrap(username);
        return data == null ? cachedUserNotFound() : cachedSuccess(data, "Portfolio loaded");
    }

    @GetMapping("/api/public/portfolio/{username}/projects")
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getPublicProjects(
            @PathVariable
            @Pattern(regexp = USERNAME_PATTERN, message = "Username format is invalid")
            String username) {
        List<ProjectDto> data = portfolioService.getPublicProjects(username);
        return data == null ? cachedUserNotFound() : cachedSuccess(data, "Projects loaded");
    }

    @GetMapping("/api/public/portfolio/{username}/experience")
    public ResponseEntity<ApiResponse<List<ExperienceDto>>> getPublicExperience(
            @PathVariable
            @Pattern(regexp = USERNAME_PATTERN, message = "Username format is invalid")
            String username) {
        List<ExperienceDto> data = portfolioService.getPublicExperience(username);
        return data == null ? cachedUserNotFound() : cachedSuccess(data, "Experience loaded");
    }

    @GetMapping("/api/public/portfolio/{username}/skills")
    public ResponseEntity<ApiResponse<List<SkillDto>>> getPublicSkills(
            @PathVariable
            @Pattern(regexp = USERNAME_PATTERN, message = "Username format is invalid")
            String username) {
        List<SkillDto> data = portfolioService.getPublicSkills(username);
        return data == null ? cachedUserNotFound() : cachedSuccess(data, "Skills loaded");
    }

    @GetMapping("/api/public/portfolio/{username}/education")
    public ResponseEntity<ApiResponse<List<EducationDto>>> getPublicEducation(
            @PathVariable
            @Pattern(regexp = USERNAME_PATTERN, message = "Username format is invalid")
            String username) {
        List<EducationDto> data = portfolioService.getPublicEducation(username);
        return data == null ? cachedUserNotFound() : cachedSuccess(data, "Education loaded");
    }

    @GetMapping("/api/public/portfolio/{username}/resume")
    public ResponseEntity<ApiResponse<ResumeDto>> getPublicResume(
            @PathVariable
            @Pattern(regexp = USERNAME_PATTERN, message = "Username format is invalid")
            String username) {
        ResumeDto data = portfolioService.getPublicResume(username);
        return data == null && !portfolioService.publicUserExists(username)
                ? cachedUserNotFound()
                : cachedSuccess(data, "Resume loaded");
    }

    @GetMapping("/api/public/portfolio/{username}/contact")
    public ResponseEntity<ApiResponse<ContactDto>> getPublicContact(
            @PathVariable
            @Pattern(regexp = USERNAME_PATTERN, message = "Username format is invalid")
            String username) {
        ContactDto data = portfolioService.getPublicContact(username);
        return data == null && !portfolioService.publicUserExists(username)
                ? cachedUserNotFound()
                : cachedSuccess(data, "Contact loaded");
    }
}
