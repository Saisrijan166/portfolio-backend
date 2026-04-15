package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.ResumeAiBulkDeleteRequestDto;
import com.srijan.portfolio.dto.ResumeAiBulkInsertRequestDto;
import com.srijan.portfolio.dto.ResumeAiBulkMutationResponseDto;
import com.srijan.portfolio.dto.ResumeJobResponseDto;
import com.srijan.portfolio.dto.ResumeParseRequestDto;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import com.srijan.portfolio.dto.ResumeRegenerateRequestDto;
import com.srijan.portfolio.dto.ResumeScoreDto;
import com.srijan.portfolio.service.PortfolioService;
import com.srijan.portfolio.service.ResumeJobService;
import com.srijan.portfolio.service.ResumeParseService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/resume")
public class ResumeParseController {

    private final PortfolioService portfolioService;
    private final ResumeParseService resumeParseService;
    private final ResumeJobService resumeJobService;

    /**
     * Legacy synchronous endpoint kept for backward compatibility.
     */
    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<ResumeParseResponseDto>> parseResume(
            Authentication auth,
            @Valid @RequestBody ResumeParseRequestDto request) {
        requireAuthenticatedUsername(auth);
        ResumeParseResponseDto result = resumeParseService.parseResume(
                request.getFileBase64(),
                request.getFileType(),
                request.getFileName());

        return ApiResponses.ok(result, "Resume parsed successfully");
    }

    @PostMapping("/parse/jobs")
    public ResponseEntity<ApiResponse<ResumeJobResponseDto>> createParseJob(
            Authentication auth,
            @Valid @RequestBody ResumeParseRequestDto request) {
        requireAuthenticatedUsername(auth);
        return ApiResponses.ok(
                resumeJobService.createJob(auth.getName(), request),
                "Resume parsing job created"
        );
    }

    @GetMapping("/job/{id}")
    public ResponseEntity<ApiResponse<ResumeJobResponseDto>> getResumeJob(
            Authentication auth,
            @PathVariable Long id) {
        requireAuthenticatedUsername(auth);
        return ApiResponses.ok(
                resumeJobService.getJob(auth.getName(), id),
                "Resume job fetched successfully"
        );
    }

    @PostMapping("/regenerate")
    public ResponseEntity<ApiResponse<ResumeParseResponseDto>> regenerateSelectedSections(
            Authentication auth,
            @Valid @RequestBody ResumeRegenerateRequestDto request) {
        requireAuthenticatedUsername(auth);
        return ApiResponses.ok(
                resumeParseService.regenerateSections(request),
                "Selected resume sections regenerated successfully"
        );
    }

    @PostMapping("/score")
    public ResponseEntity<ApiResponse<ResumeScoreDto>> scoreResume(
            Authentication auth,
            @Valid @RequestBody ResumeParseResponseDto request) {
        requireAuthenticatedUsername(auth);
        return ApiResponses.ok(
                resumeParseService.scoreResume(request),
                "Resume scored successfully"
        );
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<ApiResponse<ResumeAiBulkMutationResponseDto>> bulkDelete(
            Authentication auth,
            @Valid @RequestBody ResumeAiBulkDeleteRequestDto request) {
        requireAuthenticatedUsername(auth);
        return ApiResponses.ok(
                portfolioService.bulkDeleteResumeAiSections(auth.getName(), request.getSections()),
                "Resume AI bulk delete completed"
        );
    }

    @PostMapping("/bulk-insert")
    public ResponseEntity<ApiResponse<ResumeAiBulkMutationResponseDto>> bulkInsert(
            Authentication auth,
            @Valid @RequestBody ResumeAiBulkInsertRequestDto request) {
        requireAuthenticatedUsername(auth);
        return ApiResponses.ok(
                portfolioService.bulkInsertResumeAiSections(auth.getName(), request),
                "Resume AI bulk insert completed"
        );
    }

    @PostMapping("/bulk-replace")
    public ResponseEntity<ApiResponse<ResumeAiBulkMutationResponseDto>> bulkReplace(
            Authentication auth,
            @Valid @RequestBody ResumeAiBulkInsertRequestDto request) {
        requireAuthenticatedUsername(auth);
        return ApiResponses.ok(
                portfolioService.bulkReplaceResumeAiSections(auth.getName(), request),
                "Resume AI bulk replace completed"
        );
    }

    private String requireAuthenticatedUsername(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new com.srijan.portfolio.exception.ApiException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Authentication is required"
            );
        }
        return auth.getName();
    }
}
