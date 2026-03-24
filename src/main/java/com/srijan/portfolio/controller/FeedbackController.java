package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.service.FeedbackService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/me/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminFeedbackDashboardDto>> getFeedbackDashboard(
            Authentication auth,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponses.ok(
                feedbackService.getAdminFeedbackDashboard(auth.getName(), page, size),
                "Feedback loaded"
        );
    }

    @PostMapping("/platform")
    public ResponseEntity<ApiResponse<FeedbackSubmissionResponse>> submitPlatformFeedback(
            Authentication auth,
            @Valid @RequestBody FeedbackSubmitRequest request) {
        return ApiResponses.ok(
                feedbackService.submitPlatformFeedback(auth.getName(), request),
                "Feedback sent"
        );
    }
}
