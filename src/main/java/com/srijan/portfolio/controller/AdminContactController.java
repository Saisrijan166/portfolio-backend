package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.ContactDto;
import com.srijan.portfolio.service.PortfolioService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Admin: Contact management  (/api/me/contact)
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/me/contact")
public class AdminContactController {

    private final PortfolioService portfolioService;

    private String username(Authentication auth) {
        return auth.getName();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ContactDto>> getContact(Authentication auth) {
        return ApiResponses.ok(portfolioService.getMyContact(username(auth)), "Contact loaded");
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ContactDto>> updateContact(
            Authentication auth,
            @Valid @RequestBody ContactDto dto) {
        return ApiResponses.ok(portfolioService.updateContact(username(auth), dto), "Contact updated");
    }
}
