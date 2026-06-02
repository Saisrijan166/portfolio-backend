package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.TemplateDto;
import com.srijan.portfolio.dto.TemplateUpdateRequest;
import com.srijan.portfolio.service.AccountService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/me/template")
@RequiredArgsConstructor
public class TemplateController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<ApiResponse<TemplateDto>> getTemplate(Authentication auth) {
        return ApiResponses.ok(accountService.getTemplate(auth.getName()), "Template loaded");
    }

    @PutMapping
    public ResponseEntity<ApiResponse<TemplateDto>> updateTemplate(
            Authentication auth,
            @Valid @RequestBody TemplateUpdateRequest request) {
        return ApiResponses.ok(accountService.updateTemplate(auth.getName(), request), "Template updated");
    }
}
