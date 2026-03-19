package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.AboutDto;
import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.service.AboutService;
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
@RequiredArgsConstructor
@RequestMapping({"/api/admin/about", "/api/me/about"})
public class AdminAboutController {

    private final AboutService aboutService;

    @GetMapping
    public ResponseEntity<ApiResponse<AboutDto>> getAbout(Authentication auth) {
        return ApiResponses.ok(aboutService.getMyAbout(auth.getName()), "About loaded");
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AboutDto>> updateAbout(
            Authentication auth,
            @Valid @RequestBody AboutDto dto) {
        return ApiResponses.ok(aboutService.updateAbout(auth.getName(), dto), "About updated");
    }
}
