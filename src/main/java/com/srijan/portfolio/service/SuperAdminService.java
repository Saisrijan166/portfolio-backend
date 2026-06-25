package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.email.EmailService;
import com.srijan.portfolio.email.TenantBrandingResolver;
import com.srijan.portfolio.email.TenantEmailContext;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.entity.UserStatus;
import com.srijan.portfolio.exception.ForbiddenException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.AuthProviderRepository;
import com.srijan.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SuperAdminService — handles all superadmin user management operations.
 *
 * Security: Every public method validates the caller is a configured superadmin
 * via the SUPERADMIN_USERNAMES environment variable. This is a server-side
 * allowlist check — completely independent of frontend env vars.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final EmailService emailService;
    private final TenantBrandingResolver tenantBrandingResolver;

    @Value("${superadmin.usernames:}")
    private String superadminUsernames;

    // -------------------------------------------------------------------------
    // Security: server-side superadmin validation
    // -------------------------------------------------------------------------

    public void validateSuperAdmin(String username) {
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("SUPERADMIN_REQUIRED", "Superadmin access required");
        }

        Set<String> allowed = parseSuperadminUsernames();
        if (!allowed.contains(username.toLowerCase(Locale.ROOT))) {
            log.warn("Superadmin access denied for username: {}", username);
            throw new ForbiddenException("SUPERADMIN_REQUIRED", "Superadmin access required");
        }
    }

    private Set<String> parseSuperadminUsernames() {
        if (superadminUsernames == null || superadminUsernames.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(superadminUsernames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    // -------------------------------------------------------------------------
    // User listing with server-side filtering, sorting, and pagination
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SuperAdminUserPageDto listUsers(
            String callerUsername,
            int page,
            int size,
            String sortBy,
            String sortDir,
            String searchQuery,
            String statusFilter,
            String roleFilter,
            Boolean emailVerifiedFilter,
            String providerFilter
    ) {
        validateSuperAdmin(callerUsername);

        // Clamp page size to prevent abuse
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        Sort sort = resolveSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Use the repo's findAll and apply in-memory filtering
        // For production scale, this should be a Specification-based query.
        // Given the portfolio platform scale (likely < 10k users), this is acceptable.
        Page<User> userPage = userRepository.findAll(pageable);

        // Build filter predicate for post-query filtering when needed
        List<User> filteredAll = null;
        boolean needsFiltering = hasActiveFilter(searchQuery, statusFilter, roleFilter, emailVerifiedFilter, providerFilter);

        if (needsFiltering) {
            // Re-fetch all for filtering and re-paginate
            List<User> allUsers = userRepository.findAll(sort);
            filteredAll = allUsers.stream()
                    .filter(u -> matchesSearchQuery(u, searchQuery))
                    .filter(u -> matchesStatus(u, statusFilter))
                    .filter(u -> matchesRole(u, roleFilter))
                    .filter(u -> matchesEmailVerified(u, emailVerifiedFilter))
                    .filter(u -> matchesProvider(u, providerFilter))
                    .toList();

            int totalFiltered = filteredAll.size();
            int fromIndex = Math.min(page * size, totalFiltered);
            int toIndex = Math.min(fromIndex + size, totalFiltered);
            List<User> pageSlice = filteredAll.subList(fromIndex, toIndex);

            return SuperAdminUserPageDto.builder()
                    .users(pageSlice.stream().map(this::toDto).toList())
                    .totalElements(totalFiltered)
                    .totalPages((int) Math.ceil((double) totalFiltered / size))
                    .currentPage(page)
                    .pageSize(size)
                    .stats(buildStats())
                    .build();
        }

        return SuperAdminUserPageDto.builder()
                .users(userPage.getContent().stream().map(this::toDto).toList())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .stats(buildStats())
                .build();
    }

    // -------------------------------------------------------------------------
    // Single user details
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SuperAdminUserDto getUser(String callerUsername, Long userId) {
        validateSuperAdmin(callerUsername);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return toDto(user);
    }

    // -------------------------------------------------------------------------
    // Update user (status, role, emailVerified)
    // -------------------------------------------------------------------------

    @Transactional
    public SuperAdminUserDto updateUser(String callerUsername, Long userId, SuperAdminUpdateUserRequest request) {
        validateSuperAdmin(callerUsername);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Prevent superadmin from modifying their own critical fields
        if (user.getUsername().equalsIgnoreCase(callerUsername)) {
            throw new ForbiddenException("SELF_MODIFY_DENIED", "Cannot modify your own account via superadmin panel");
        }

        if (request.getStatus() != null) {
            try {
                UserStatus status = UserStatus.valueOf(request.getStatus().toUpperCase(Locale.ROOT));
                user.setStatus(status);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status value: " + request.getStatus());
            }
        }

        if (request.getRole() != null) {
            String normalizedRole = request.getRole().toUpperCase(Locale.ROOT);
            if (!normalizedRole.startsWith("ROLE_")) {
                normalizedRole = "ROLE_" + normalizedRole;
            }
            if (!normalizedRole.equals("ROLE_USER") && !normalizedRole.equals("ROLE_ADMIN")) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
            user.setRole(normalizedRole);
        }

        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }

        User saved = userRepository.save(user);
        log.info("Superadmin {} updated user {} (id={})", callerUsername, saved.getUsername(), saved.getId());
        return toDto(saved);
    }

    // -------------------------------------------------------------------------
    // Bulk actions (suspend, activate, verify email)
    // -------------------------------------------------------------------------

    @Transactional
    public Map<String, Object> bulkAction(String callerUsername, SuperAdminBulkActionRequest request) {
        validateSuperAdmin(callerUsername);

        List<Long> userIds = request.getUserIds().stream()
                .distinct()
                .toList();

        List<User> users = userRepository.findAllById(userIds);

        // Remove caller from the list to prevent self-modification
        users = users.stream()
                .filter(u -> !u.getUsername().equalsIgnoreCase(callerUsername))
                .toList();

        int affected = 0;
        for (User user : users) {
            switch (request.getAction()) {
                case SUSPEND -> {
                    user.setStatus(UserStatus.SUSPENDED);
                    affected++;
                }
                case ACTIVATE -> {
                    user.setStatus(UserStatus.ACTIVE);
                    affected++;
                }
            }
        }

        userRepository.saveAll(users);
        log.info("Superadmin {} performed bulk {} on {} users", callerUsername, request.getAction(), affected);

        return Map.of(
                "action", request.getAction().name(),
                "requested", userIds.size(),
                "affected", affected
        );
    }

    // -------------------------------------------------------------------------
    // Bulk email sending
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<String, Object> bulkEmail(String callerUsername, SuperAdminBulkEmailRequest request) {
        validateSuperAdmin(callerUsername);

        List<Long> userIds = request.getUserIds().stream()
                .distinct()
                .toList();

        List<User> users = userRepository.findAllById(userIds);

        // Only send to users with valid email addresses
        List<User> emailableUsers = users.stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .toList();

        TenantEmailContext context = tenantBrandingResolver.resolveCurrent();

        int sent = 0;
        int skipped = 0;
        for (User user : emailableUsers) {
            try {
                // Ensure greeting maintains consistent paragraph styling
                String greeting = "<p style=\"margin: 0 0 14px; font-size: 14px; line-height: 1.7; color: #374151;\">Hi <b>" + user.getUsername() + "</b>,</p>";
                String personalizedBody = greeting + request.getBody();

                emailService.sendAnnouncementEmail(
                        user.getEmail(),
                        request.getSubject(),
                        personalizedBody,
                        context
                );
                sent++;
            } catch (Exception e) {
                log.warn("Failed to send email to user {} (id={}): {}", user.getUsername(), user.getId(), e.getMessage());
                skipped++;
            }
        }

        int noEmail = users.size() - emailableUsers.size();
        log.info("Superadmin {} sent bulk email: sent={}, skipped={}, noEmail={}", callerUsername, sent, skipped, noEmail);

        return Map.of(
                "totalRequested", userIds.size(),
                "sent", sent,
                "skipped", skipped,
                "noEmail", noEmail
        );
    }

    // -------------------------------------------------------------------------
    // Stats (aggregate counts)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SuperAdminStatsDto getStats(String callerUsername) {
        validateSuperAdmin(callerUsername);
        return buildStats();
    }

    // -------------------------------------------------------------------------
    // Export (CSV data for download)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public String exportUsersCsv(String callerUsername) {
        validateSuperAdmin(callerUsername);

        List<User> all = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Username,Email,Role,Status,Email Verified,Template,Created At,Updated At\n");

        for (User u : all) {
            csv.append(u.getId()).append(",")
                    .append(escapeCsv(u.getUsername())).append(",")
                    .append(escapeCsv(u.getEmail())).append(",")
                    .append(escapeCsv(u.getRole())).append(",")
                    .append(u.getStatus().name()).append(",")
                    .append(u.isEmailVerified()).append(",")
                    .append(escapeCsv(u.getActiveTemplate())).append(",")
                    .append(u.getCreatedAt()).append(",")
                    .append(u.getUpdatedAt()).append("\n");
        }

        return csv.toString();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private SuperAdminStatsDto buildStats() {
        List<User> allUsers = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime sixMonthsAgo = now.minusMonths(6);

        return SuperAdminStatsDto.builder()
                .totalUsers(allUsers.size())
                .activeUsers(allUsers.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count())
                .suspendedUsers(allUsers.stream().filter(u -> u.getStatus() == UserStatus.SUSPENDED).count())
                .verifiedEmailUsers(allUsers.stream().filter(User::isEmailVerified).count())
                .unverifiedEmailUsers(allUsers.stream().filter(u -> !u.isEmailVerified()).count())
                .usersCreatedLast7Days(allUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(sevenDaysAgo)).count())
                .usersCreatedLast30Days(allUsers.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(sixMonthsAgo)).count())
                .build();
    }

    private SuperAdminUserDto toDto(User user) {
        List<String> providers = authProviderRepository.findAllByUserId(user.getId())
                .stream()
                .map(p -> p.getProvider().name())
                .sorted()
                .toList();

        return SuperAdminUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .hasPassword(user.getPasswordHash() != null && !user.getPasswordHash().isBlank())
                .activeTemplate(user.getActiveTemplate())
                .authProviders(providers)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private Sort resolveSort(String sortBy, String sortDir) {
        String field = switch (sortBy != null ? sortBy.toLowerCase(Locale.ROOT) : "") {
            case "username" -> "username";
            case "email" -> "email";
            case "role" -> "role";
            case "status" -> "status";
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "createdAt";
        };

        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    private boolean hasActiveFilter(String search, String status, String role, Boolean emailVerified, String provider) {
        return (search != null && !search.isBlank())
                || (status != null && !status.isBlank())
                || (role != null && !role.isBlank())
                || emailVerified != null
                || (provider != null && !provider.isBlank());
    }

    private boolean matchesSearchQuery(User user, String query) {
        if (query == null || query.isBlank()) return true;
        String lq = query.toLowerCase(Locale.ROOT);
        return (user.getUsername() != null && user.getUsername().toLowerCase(Locale.ROOT).contains(lq))
                || (user.getEmail() != null && user.getEmail().toLowerCase(Locale.ROOT).contains(lq))
                || (user.getId() != null && user.getId().toString().contains(lq));
    }

    private boolean matchesStatus(User user, String status) {
        if (status == null || status.isBlank()) return true;
        return user.getStatus().name().equalsIgnoreCase(status);
    }

    private boolean matchesRole(User user, String role) {
        if (role == null || role.isBlank()) return true;
        return user.getRole() != null && user.getRole().equalsIgnoreCase(role);
    }

    private boolean matchesEmailVerified(User user, Boolean emailVerified) {
        if (emailVerified == null) return true;
        return user.isEmailVerified() == emailVerified;
    }

    private boolean matchesProvider(User user, String provider) {
        if (provider == null || provider.isBlank()) return true;
        List<String> providers = authProviderRepository.findAllByUserId(user.getId())
                .stream()
                .map(p -> p.getProvider().name())
                .toList();
        return providers.stream().anyMatch(p -> p.equalsIgnoreCase(provider));
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
