package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.AccountDto;
import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.dto.ChangePasswordRequest;
import com.srijan.portfolio.dto.ChangeUsernameRequest;
import com.srijan.portfolio.dto.UsernameAvailabilityDto;
import com.srijan.portfolio.service.AccountService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/me/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<ApiResponse<AccountDto>> getAccount(Authentication auth) {
        return ApiResponses.ok(accountService.getMyAccount(auth.getName()), "Account loaded");
    }

    @GetMapping("/username-availability")
    public ResponseEntity<ApiResponse<UsernameAvailabilityDto>> checkUsernameAvailability(
            Authentication auth,
            @RequestParam("username")
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,30}$", message = "Username must be 3-31 chars using lowercase letters, numbers, or hyphens")
            String username) {
        return ApiResponses.ok(
                accountService.checkUsernameAvailability(auth.getName(), username),
                "Username availability checked"
        );
    }

    @PutMapping("/username")
    public ResponseEntity<ApiResponse<AccountDto>> changeUsername(
            Authentication auth,
            @Valid @RequestBody ChangeUsernameRequest request) {
        return ApiResponses.ok(accountService.changeUsername(auth.getName(), request), "Username updated");
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<AccountDto>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ApiResponses.ok(accountService.changePassword(auth.getName(), request), "Password updated");
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<AccountDto>> markEmailVerified(Authentication auth) {
        return ApiResponses.ok(accountService.markEmailVerified(auth.getName()), "Email marked as verified");
    }
}
