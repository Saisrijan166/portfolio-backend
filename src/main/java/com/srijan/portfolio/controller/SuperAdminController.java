package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.service.SuperAdminService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SuperAdminController — REST endpoints for superadmin user management.
 *
 * All endpoints are under /api/superadmin/** and require:
 *   1. A valid JWT (handled by Spring Security filter chain)
 *   2. ROLE_USER or ROLE_ADMIN role (Spring Security authorization)
 *   3. Username must be in the server-side SUPERADMIN_USERNAMES allowlist
 *      (checked by SuperAdminService on every call)
 *
 * This triple-layer security ensures no one can access these endpoints
 * without being explicitly configured as a superadmin on the server.
 */
@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    // -------------------------------------------------------------------------
    // Verify superadmin access (used by frontend for gate check)
    // -------------------------------------------------------------------------

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifySuperAdmin(
            Authentication auth,
            @RequestBody Map<String, String> body
    ) {
        String pin = body != null ? body.get("pin") : null;
        superAdminService.validateSuperAdminWithPin(auth.getName(), pin);
        return ApiResponses.ok(Map.of("superadmin", true), "Superadmin verified");
    }

    // -------------------------------------------------------------------------
    // List users with filtering, sorting, pagination
    // -------------------------------------------------------------------------

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<SuperAdminUserPageDto>> listUsers(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) String provider
    ) {
        SuperAdminUserPageDto result = superAdminService.listUsers(
                auth.getName(), page, size, sortBy, sortDir,
                search, status, role, emailVerified, provider
        );
        return ApiResponses.ok(result, "Users fetched");
    }

    // -------------------------------------------------------------------------
    // Get single user details
    // -------------------------------------------------------------------------

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<SuperAdminUserDto>> getUser(
            Authentication auth,
            @PathVariable Long userId
    ) {
        return ApiResponses.ok(superAdminService.getUser(auth.getName(), userId), "User fetched");
    }

    // -------------------------------------------------------------------------
    // Update user (status, role, emailVerified)
    // -------------------------------------------------------------------------

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<SuperAdminUserDto>> updateUser(
            Authentication auth,
            @PathVariable Long userId,
            @Valid @RequestBody SuperAdminUpdateUserRequest request
    ) {
        return ApiResponses.ok(superAdminService.updateUser(auth.getName(), userId, request), "User updated");
    }

    // -------------------------------------------------------------------------
    // Bulk actions (suspend, activate, verify email)
    // -------------------------------------------------------------------------

    @PostMapping("/users/bulk-action")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkAction(
            Authentication auth,
            @Valid @RequestBody SuperAdminBulkActionRequest request
    ) {
        return ApiResponses.ok(superAdminService.bulkAction(auth.getName(), request), "Bulk action completed");
    }

    // -------------------------------------------------------------------------
    // Bulk email
    // -------------------------------------------------------------------------

    @PostMapping("/users/bulk-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkEmail(
            Authentication auth,
            @Valid @RequestBody SuperAdminBulkEmailRequest request
    ) {
        return ApiResponses.ok(superAdminService.bulkEmail(auth.getName(), request), "Bulk email sent");
    }

    // -------------------------------------------------------------------------
    // Stats
    // -------------------------------------------------------------------------

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SuperAdminStatsDto>> getStats(Authentication auth) {
        return ApiResponses.ok(superAdminService.getStats(auth.getName()), "Stats fetched");
    }

    // -------------------------------------------------------------------------
    // CSV export
    // -------------------------------------------------------------------------

    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportCsv(Authentication auth) {
        String csv = superAdminService.exportUsersCsv(auth.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users_export.csv")
                .body(csv.getBytes());
    }
}
