package com.srijan.portfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AiProviderConfig {

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${GROQ_SUMMARIZE_API_KEY:${GROQ_API_KEY:}}")
    private String groqApiKey;

    /**
     * Model identifiers are configuration, not constants. Providers retire model names on their own
     * schedule (a retired name answers 404), and when that happens the fix must be an environment
     * change rather than a code change and redeploy.
     */
    @Value("${GEMINI_MODEL:gemini-flash-latest}")
    private String geminiModel;

    @Value("${GROQ_MODEL:openai/gpt-oss-120b}")
    private String groqModel;

    /**
     * Completion budget for Groq. Reasoning-style models spend part of this budget before emitting
     * any content, so a resume-sized JSON response needs materially more than a plain chat reply.
     */
    @Value("${GROQ_MAX_TOKENS:8192}")
    private int groqMaxTokens;

    @Value("${AI_TIMEOUT_MS:60000}")
    private int timeoutMs;

    @Value("${AI_PROVIDER_PRIORITY:gemini,groq}")
    private String providerPriority;

    @Value("${AI_PROVIDER_RETRY_ATTEMPTS:2}")
    private int maxRetryAttempts;

    /**
     * Pause before re-attempting the same provider. Both providers signal overload by status —
     * Gemini with 503 and Groq with 429 — and an immediate retry is the one thing guaranteed not
     * to help. Scaled by attempt number, so the second attempt waits longer than the first.
     */
    @Value("${AI_PROVIDER_RETRY_BACKOFF_MS:800}")
    private long retryBackoffMs;

    @Value("${RESUME_JOB_POLL_TIMEOUT_MS:180000}")
    private long jobTimeoutMs;

    @Value("${RESUME_MAX_SIZE_BYTES:2097152}")
    private long maxFileSize; // 2MB default

    @Value("${RESUME_ALLOWED_TYPES:pdf,docx}")
    private String allowedTypes;

    @Value("${AI_SECTION_REGEN_MAX_SECTIONS:5}")
    private int maxRegeneratedSections;

    public boolean isGeminiAvailable() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    public boolean isGroqAvailable() {
        return groqApiKey != null && !groqApiKey.isBlank();
    }

    /** Used by AI providers to authenticate with external APIs */
    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    /** Used by AI providers to authenticate with external APIs */
    public String getGroqApiKey() {
        return groqApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public String getGroqModel() {
        return groqModel;
    }

    public int getGroqMaxTokens() {
        return Math.max(256, groqMaxTokens);
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public List<String> getProviderPriority() {
        return Arrays.stream(providerPriority.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .toList();
    }

    public int getMaxRetryAttempts() {
        return Math.max(1, maxRetryAttempts);
    }

    public long getRetryBackoffMs() {
        return Math.max(0L, retryBackoffMs);
    }

    public long getJobTimeoutMs() {
        return Math.max(1000L, jobTimeoutMs);
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public List<String> getAllowedTypes() {
        return Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .toList();
    }

    public int getMaxRegeneratedSections() {
        return Math.max(1, maxRegeneratedSections);
    }
}
