package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.AppearanceSettingsDto;
import com.srijan.portfolio.service.AppearanceConfigService;
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
@RequestMapping({"/api/admin/appearance", "/api/me/appearance"})
public class AdminAppearanceController {

    private final AppearanceConfigService appearanceConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<AppearanceSettingsDto>> getAppearance(Authentication auth) {
        return ApiResponses.ok(appearanceConfigService.getMyAppearance(auth.getName()), "Appearance loaded");
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AppearanceSettingsDto>> updateAppearance(
            Authentication auth,
            @Valid @RequestBody AppearanceSettingsDto dto) {
        return ApiResponses.ok(appearanceConfigService.updateAppearance(auth.getName(), dto), "Appearance updated");
    }
}
