package com.srijan.portfolio.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single source of truth for per-endpoint request limits.
 *
 * <p>Every rule resolves to a stable bucket name so that path variables (usernames, ids) can never
 * be used to sidestep a limit by varying the URL. Limits are per client IP, per one-minute window,
 * and are externalised as properties so they can be retuned without a code change.
 */
@Component
public class RateLimitPolicy {

    /** A resolved limit for one request. */
    public record Rule(String bucket, int limit) {
    }

    private static final Pattern PUBLIC_PORTFOLIO = Pattern.compile("^/api/public/portfolio/[^/]+(/.*)?$");

    @Value("${security.rate-limit.global:600}")
    private int globalLimit;

    @Value("${security.rate-limit.auth-credentials:15}")
    private int authCredentialsLimit;

    @Value("${security.rate-limit.auth-otp-send:5}")
    private int authOtpSendLimit;

    @Value("${security.rate-limit.auth-otp-verify:10}")
    private int authOtpVerifyLimit;

    @Value("${security.rate-limit.auth-session:30}")
    private int authSessionLimit;

    @Value("${security.rate-limit.superadmin-verify:5}")
    private int superadminVerifyLimit;

    @Value("${security.rate-limit.superadmin-bulk-email:3}")
    private int superadminBulkEmailLimit;

    @Value("${security.rate-limit.superadmin-bulk:10}")
    private int superadminBulkLimit;

    @Value("${security.rate-limit.superadmin:60}")
    private int superadminLimit;

    @Value("${security.rate-limit.cron:12}")
    private int cronLimit;

    @Value("${security.rate-limit.public-contact-message:5}")
    private int publicContactMessageLimit;

    @Value("${security.rate-limit.public-feedback:15}")
    private int publicFeedbackLimit;

    @Value("${security.rate-limit.public-resume-generate:10}")
    private int publicResumeGenerateLimit;

    @Value("${security.rate-limit.public-read:200}")
    private int publicReadLimit;

    @Value("${security.rate-limit.admin-feedback:40}")
    private int adminFeedbackLimit;

    @Value("${security.rate-limit.account-username-check:30}")
    private int accountUsernameCheckLimit;

    @Value("${security.rate-limit.account-security:10}")
    private int accountSecurityLimit;

    @Value("${security.rate-limit.resume-bulk:20}")
    private int resumeBulkLimit;

    @Value("${security.rate-limit.resume-job-poll:120}")
    private int resumeJobPollLimit;

    @Value("${security.rate-limit.keep-warm:60}")
    private int keepWarmLimit;

    @Value("${security.rate-limit.default:300}")
    private int defaultLimit;

    /** Aggregate ceiling applied per client IP across every endpoint. */
    public int globalLimit() {
        return globalLimit;
    }

    /** Platform health probes must never be throttled. */
    public boolean isExempt(String path) {
        return path == null || path.startsWith("/actuator");
    }

    public Rule resolve(String method, String path) {
        if (path.startsWith("/api/auth/")) {
            return resolveAuth(path);
        }
        if (path.startsWith("/api/superadmin/")) {
            return resolveSuperAdmin(path);
        }
        if (path.startsWith("/api/cron/")) {
            return new Rule("cron", cronLimit);
        }
        if (path.startsWith("/api/public/")) {
            return resolvePublic(path);
        }
        if (path.startsWith("/api/me/")) {
            return resolveMe(path);
        }
        if (path.equals("/api/users/count") || path.equals("/api/users/health")) {
            return new Rule("keep-warm", keepWarmLimit);
        }
        return new Rule("default:" + method, defaultLimit);
    }

    private Rule resolveAuth(String path) {
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            return new Rule("auth-credentials", authCredentialsLimit);
        }
        // Both of these send an email, so they are the cheapest abuse vector in the app.
        if (path.equals("/api/auth/otp/request") || path.equals("/api/auth/password/forgot")) {
            return new Rule("auth-otp-send", authOtpSendLimit);
        }
        if (path.equals("/api/auth/otp/verify") || path.equals("/api/auth/password/reset")) {
            return new Rule("auth-otp-verify", authOtpVerifyLimit);
        }
        return new Rule("auth-session", authSessionLimit);
    }

    private Rule resolveSuperAdmin(String path) {
        if (path.equals("/api/superadmin/verify")) {
            return new Rule("superadmin-verify", superadminVerifyLimit);
        }
        if (path.equals("/api/superadmin/users/bulk-email")) {
            return new Rule("superadmin-bulk-email", superadminBulkEmailLimit);
        }
        if (path.equals("/api/superadmin/users/bulk-action") || path.equals("/api/superadmin/users/export")) {
            return new Rule("superadmin-bulk", superadminBulkLimit);
        }
        return new Rule("superadmin", superadminLimit);
    }

    private Rule resolvePublic(String path) {
        String suffix = publicPortfolioSuffix(path);
        if (suffix != null) {
            if (suffix.equals("/contact/message")) {
                return new Rule("public-contact-message", publicContactMessageLimit);
            }
            if (suffix.equals("/feedback") || suffix.equals("/feedback/platform")) {
                return new Rule("public-feedback", publicFeedbackLimit);
            }
            if (suffix.startsWith("/resume/generate")) {
                return new Rule("public-resume-generate", publicResumeGenerateLimit);
            }
        }
        return new Rule("public-read", publicReadLimit);
    }

    private Rule resolveMe(String path) {
        if (path.startsWith("/api/me/feedback")) {
            return new Rule("admin-feedback", adminFeedbackLimit);
        }
        if (path.equals("/api/me/account/username-availability")) {
            return new Rule("account-username-check", accountUsernameCheckLimit);
        }
        if (path.equals("/api/me/account/username")
                || path.equals("/api/me/account/password")
                || path.equals("/api/me/account/email/verify")) {
            return new Rule("account-security", accountSecurityLimit);
        }
        if (path.startsWith("/api/me/resume/bulk-")) {
            return new Rule("resume-bulk", resumeBulkLimit);
        }
        if (path.startsWith("/api/me/resume/job/")) {
            return new Rule("resume-job-poll", resumeJobPollLimit);
        }
        return new Rule("me", defaultLimit);
    }

    /**
     * Returns the portion of a public portfolio path after the username, or {@code null} when the
     * path is not a public portfolio path. Collapsing the username keeps the bucket key space bound
     * to the number of rules rather than the number of usernames an attacker can invent.
     */
    private String publicPortfolioSuffix(String path) {
        Matcher matcher = PUBLIC_PORTFOLIO.matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        String suffix = matcher.group(1);
        return suffix == null ? "" : suffix;
    }
}
