package com.srijan.portfolio.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingTest {

    // -------------------------------------------------------------------------
    // ClientIpFilter — the forwarded-for entry must be counted from the right
    // -------------------------------------------------------------------------

    @Test
    void ignoresClientSuppliedForwardedForEntries() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.7");

        runClientIpFilter(request, true, 1);

        assertThat(ClientIpFilter.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void usesSocketAddressWhenProxyHeadersAreNotTrusted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        runClientIpFilter(request, false, 1);

        assertThat(ClientIpFilter.resolve(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void rejectsForgedForwardedForValuesThatAreNotAddresses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "not-an-ip-".repeat(20));

        runClientIpFilter(request, true, 1);

        assertThat(ClientIpFilter.resolve(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void stripsPortFromForwardedForEntry() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7:54321");

        runClientIpFilter(request, true, 1);

        assertThat(ClientIpFilter.resolve(request)).isEqualTo("203.0.113.7");
    }

    private void runClientIpFilter(MockHttpServletRequest request, boolean trustProxy, int proxyCount)
            throws Exception {
        new ClientIpFilter(trustProxy, proxyCount)
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    // -------------------------------------------------------------------------
    // RateLimitPolicy — path variables must not create new buckets
    // -------------------------------------------------------------------------

    @Test
    void collapsesUsernameSoPublicBucketsAreShared() {
        RateLimitPolicy policy = policy();

        RateLimitPolicy.Rule first = policy.resolve("POST", "/api/public/portfolio/alice/contact/message");
        RateLimitPolicy.Rule second = policy.resolve("POST", "/api/public/portfolio/bob/contact/message");

        assertThat(first.bucket()).isEqualTo(second.bucket());
        assertThat(first.limit()).isEqualTo(5);
    }

    @Test
    void appliesDedicatedLimitsToExpensiveEndpoints() {
        RateLimitPolicy policy = policy();

        assertThat(policy.resolve("GET", "/api/public/portfolio/alice/resume/generate").bucket())
                .isEqualTo("public-resume-generate");
        assertThat(policy.resolve("GET", "/api/public/portfolio/alice/resume/generate/tex").limit())
                .isEqualTo(10);
        assertThat(policy.resolve("POST", "/api/auth/otp/request").limit()).isEqualTo(5);
        assertThat(policy.resolve("POST", "/api/superadmin/verify").limit()).isEqualTo(5);
        assertThat(policy.resolve("POST", "/api/cron/weekly-email").bucket()).isEqualTo("cron");
        assertThat(policy.resolve("GET", "/api/public/portfolio/alice").bucket()).isEqualTo("public-read");
    }

    @Test
    void exemptsHealthProbes() {
        assertThat(policy().isExempt("/actuator/health")).isTrue();
        assertThat(policy().isExempt("/api/public/portfolio/alice")).isFalse();
    }

    private RateLimitPolicy policy() {
        RateLimitPolicy policy = new RateLimitPolicy();
        // Mirrors the defaults declared on the @Value fields, which are not applied outside a context.
        ReflectionTestUtils.setField(policy, "globalLimit", 600);
        ReflectionTestUtils.setField(policy, "authCredentialsLimit", 15);
        ReflectionTestUtils.setField(policy, "authOtpSendLimit", 5);
        ReflectionTestUtils.setField(policy, "authOtpVerifyLimit", 10);
        ReflectionTestUtils.setField(policy, "authSessionLimit", 30);
        ReflectionTestUtils.setField(policy, "superadminVerifyLimit", 5);
        ReflectionTestUtils.setField(policy, "superadminBulkEmailLimit", 3);
        ReflectionTestUtils.setField(policy, "superadminBulkLimit", 10);
        ReflectionTestUtils.setField(policy, "superadminLimit", 60);
        ReflectionTestUtils.setField(policy, "cronLimit", 12);
        ReflectionTestUtils.setField(policy, "publicContactMessageLimit", 5);
        ReflectionTestUtils.setField(policy, "publicFeedbackLimit", 15);
        ReflectionTestUtils.setField(policy, "publicResumeGenerateLimit", 10);
        ReflectionTestUtils.setField(policy, "publicReadLimit", 200);
        ReflectionTestUtils.setField(policy, "adminFeedbackLimit", 40);
        ReflectionTestUtils.setField(policy, "accountUsernameCheckLimit", 30);
        ReflectionTestUtils.setField(policy, "accountSecurityLimit", 10);
        ReflectionTestUtils.setField(policy, "resumeBulkLimit", 20);
        ReflectionTestUtils.setField(policy, "resumeJobPollLimit", 120);
        ReflectionTestUtils.setField(policy, "keepWarmLimit", 60);
        ReflectionTestUtils.setField(policy, "defaultLimit", 300);
        return policy;
    }

    // -------------------------------------------------------------------------
    // FixedWindowRateLimiter — counting, eviction, and the fail-open guard
    // -------------------------------------------------------------------------

    @Test
    void blocksOnlyAfterTheLimitIsExceeded() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(Duration.ofMinutes(1), 1_000, 100);

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.hit("k", 3).exceeded()).isFalse();
        }

        FixedWindowRateLimiter.Hit blocked = limiter.hit("k", 3);
        assertThat(blocked.exceeded()).isTrue();
        assertThat(blocked.remaining()).isZero();
        assertThat(blocked.retryAfterSeconds()).isPositive();
    }

    @Test
    void startsAFreshWindowOnceTheOldOneExpires() throws Exception {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(Duration.ofMillis(50), 1_000, 100);

        assertThat(limiter.hit("k", 1).exceeded()).isFalse();
        assertThat(limiter.hit("k", 1).exceeded()).isTrue();

        Thread.sleep(80);

        assertThat(limiter.hit("k", 1).exceeded()).isFalse();
    }

    @Test
    void evictsExpiredCountersInsteadOfGrowingForever() throws Exception {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(Duration.ofMillis(20), 1_000, 5);

        for (int i = 0; i < 20; i++) {
            limiter.hit("key-" + i, 100);
        }
        Thread.sleep(40);
        for (int i = 0; i < 5; i++) {
            limiter.hit("sweep-" + i, 100);
        }

        assertThat(limiter.trackedKeys()).isLessThan(20);
    }

    @Test
    void failsOpenWhenTheKeySpaceIsSaturated() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(Duration.ofMinutes(1), 2, 1_000);

        limiter.hit("a", 1);
        limiter.hit("b", 1);

        assertThat(limiter.hit("c", 1).exceeded()).isFalse();
        assertThat(limiter.trackedKeys()).isEqualTo(2);
    }
}
