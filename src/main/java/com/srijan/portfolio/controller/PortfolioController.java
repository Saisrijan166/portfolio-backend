package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    // ------------------------------------------------------------------------
    // PUBLIC READ-ONLY ENDPOINT
    // ------------------------------------------------------------------------
    @GetMapping("/api/public/portfolio/{username}")
    public ResponseEntity<PortfolioResponse> getPublicPortfolio(@PathVariable("username") String username) {
        return ResponseEntity.ok(portfolioService.getPortfolioByUsername(username));
    }

    // ------------------------------------------------------------------------
    // AUTHENTICATED USER ENDPOINTS (/api/me/**)
    // ------------------------------------------------------------------------

    // We get the current user's username from the JWT Authentication token
    private String getAuthenticatedUsername(Authentication authentication) {
        return authentication.getName();
    }

    @GetMapping("/api/me/portfolio")
    public ResponseEntity<PortfolioResponse> getMyPortfolio(Authentication authentication) {
        String username = getAuthenticatedUsername(authentication);
        return ResponseEntity.ok(portfolioService.getPortfolioByUsername(username));
    }

    @PutMapping("/api/me/portfolio/profile")
    public ResponseEntity<ProfileDto> updateMyProfile(
            Authentication authentication,
            @RequestBody ProfileDto profileDto) {
        String username = getAuthenticatedUsername(authentication);
        return ResponseEntity.ok(portfolioService.updateProfile(username, profileDto));
    }

    @PutMapping("/api/me/portfolio/projects")
    public ResponseEntity<List<ProjectDto>> updateMyProjects(
            Authentication authentication,
            @RequestBody List<ProjectDto> projectDtos) {
        String username = getAuthenticatedUsername(authentication);
        return ResponseEntity.ok(portfolioService.updateProjects(username, projectDtos));
    }

    @PutMapping("/api/me/portfolio/experiences")
    public ResponseEntity<List<ExperienceDto>> updateMyExperiences(
            Authentication authentication,
            @RequestBody List<ExperienceDto> experienceDtos) {
        String username = getAuthenticatedUsername(authentication);
        return ResponseEntity.ok(portfolioService.updateExperiences(username, experienceDtos));
    }

    @PutMapping("/api/me/portfolio/skills")
    public ResponseEntity<List<SkillDto>> updateMySkills(
            Authentication authentication,
            @RequestBody List<SkillDto> skillDtos) {
        String username = getAuthenticatedUsername(authentication);
        return ResponseEntity.ok(portfolioService.updateSkills(username, skillDtos));
    }

    @PutMapping("/api/me/portfolio/educations")
    public ResponseEntity<List<EducationDto>> updateMyEducations(
            Authentication authentication,
            @RequestBody List<EducationDto> educationDtos) {
        String username = getAuthenticatedUsername(authentication);
        return ResponseEntity.ok(portfolioService.updateEducations(username, educationDtos));
    }

    @PutMapping("/api/me/portfolio/resume")
    public ResponseEntity<ResumeDto> updateMyResume(
            Authentication authentication,
            @RequestBody ResumeDto resumeDto) {
        String username = getAuthenticatedUsername(authentication);
        return ResponseEntity.ok(portfolioService.updateResume(username, resumeDto));
    }

    @PutMapping("/api/me/portfolio/contact")
    public ResponseEntity<ContactDto> updateMyContact(
            Authentication authentication,
            @RequestBody ContactDto contactDto) {
        String username = getAuthenticatedUsername(authentication);
        return ResponseEntity.ok(portfolioService.updateContact(username, contactDto));
    }
}
