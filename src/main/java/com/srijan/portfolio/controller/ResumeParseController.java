package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.ResumeAiBulkDeleteRequestDto;
import com.srijan.portfolio.dto.ResumeAiBulkInsertRequestDto;
import com.srijan.portfolio.dto.ResumeAiBulkMutationResponseDto;
import com.srijan.portfolio.dto.ResumeParseRequestDto;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import com.srijan.portfolio.service.PortfolioService;
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

    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<ResumeParseResponseDto>> parseResume(
            Authentication auth,
            @Valid @RequestBody ResumeParseRequestDto request) {
        ResumeParseResponseDto result = resumeParseService.parseResume(
                request.getFileBase64(),
                request.getFileType(),
                request.getFileName());

        return ApiResponses.ok(result, "Resume parsed successfully");
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<ApiResponse<ResumeAiBulkMutationResponseDto>> bulkDelete(
            Authentication auth,
            @Valid @RequestBody ResumeAiBulkDeleteRequestDto request) {
        return ApiResponses.ok(
                portfolioService.bulkDeleteResumeAiSections(auth.getName(), request.getSections()),
                "Resume AI bulk delete completed"
        );
    }

    @PostMapping("/bulk-insert")
    public ResponseEntity<ApiResponse<ResumeAiBulkMutationResponseDto>> bulkInsert(
            Authentication auth,
            @Valid @RequestBody ResumeAiBulkInsertRequestDto request) {
        return ApiResponses.ok(
                portfolioService.bulkInsertResumeAiSections(auth.getName(), request),
                "Resume AI bulk insert completed"
        );
    }

    @PostMapping("/bulk-replace")
    public ResponseEntity<ApiResponse<ResumeAiBulkMutationResponseDto>> bulkReplace(
            Authentication auth,
            @Valid @RequestBody ResumeAiBulkInsertRequestDto request) {
        return ApiResponses.ok(
                portfolioService.bulkReplaceResumeAiSections(auth.getName(), request),
                "Resume AI bulk replace completed"
        );
    }
}
