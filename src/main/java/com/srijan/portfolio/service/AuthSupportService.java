package com.srijan.portfolio.service;

import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.repository.UserRepository;
import com.srijan.portfolio.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class AuthSupportService {

    private final UserRepository userRepository;

    public AuthSupportService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public String resolveIdentifier(String username, String identifier) {
        String candidate = identifier;
        if (candidate == null || candidate.isBlank()) {
            candidate = username;
        }

        if (candidate == null || candidate.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDENTIFIER_REQUIRED", "Email or username is required");
        }

        String trimmed = candidate.trim();
        return trimmed.contains("@") ? normalizeEmail(trimmed) : normalizeUsername(trimmed);
    }

    public boolean isEmailIdentifier(String identifier) {
        return identifier != null && identifier.contains("@");
    }

    public void enforceTenantAlignment(String username) {
        String tenant = TenantContext.getTenant();
        if (tenant != null && !tenant.equalsIgnoreCase(username)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "TENANT_MISMATCH",
                    "This authentication flow is not valid for the current tenant"
            );
        }
    }

    public String generateAvailableUsername(String preferredValue, String fallbackPrefix) {
        String base = slugify(preferredValue);
        if (base == null || base.isBlank()) {
            base = slugify(fallbackPrefix);
        }
        if (base == null || base.isBlank()) {
            base = "user";
        }

        if (base.length() < 3) {
            base = (base + "-user").substring(0, Math.min(31, Math.max(3, base.length() + 5)));
        }
        if (base.length() > 31) {
            base = base.substring(0, 31);
        }
        base = trimHyphens(base);
        if (base.length() < 3) {
            base = "user";
        }

        String candidate = base;
        int counter = 1;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            String suffix = "-" + counter++;
            int maxBaseLength = Math.max(3, 31 - suffix.length());
            candidate = trimHyphens(base.substring(0, Math.min(base.length(), maxBaseLength))) + suffix;
        }

        return candidate;
    }

    private String slugify(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-");

        normalized = trimHyphens(normalized);
        if (normalized.isBlank()) {
            return null;
        }

        if (!Character.isLetterOrDigit(normalized.charAt(0))) {
            normalized = "u-" + normalized;
        }

        return normalized;
    }

    private String trimHyphens(String value) {
        return value.replaceAll("^-+", "").replaceAll("-+$", "");
    }
}
