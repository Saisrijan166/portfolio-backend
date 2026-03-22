package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.DesktopWidgetsDto;
import com.srijan.portfolio.service.DesktopWidgetConfigService;
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
@RequestMapping({"/api/admin/widgets", "/api/me/widgets"})
public class AdminWidgetConfigController {

    private final DesktopWidgetConfigService desktopWidgetConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<DesktopWidgetsDto>> getWidgets(Authentication auth) {
        return ApiResponses.ok(desktopWidgetConfigService.getMyWidgets(auth.getName()), "Widgets loaded");
    }

    @PutMapping
    public ResponseEntity<ApiResponse<DesktopWidgetsDto>> updateWidgets(
            Authentication auth,
            @Valid @RequestBody DesktopWidgetsDto dto) {
        return ApiResponses.ok(desktopWidgetConfigService.updateWidgets(auth.getName(), dto), "Widgets updated");
    }
}
