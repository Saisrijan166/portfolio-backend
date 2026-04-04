package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.ResumeParseRequestDto;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
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
}
