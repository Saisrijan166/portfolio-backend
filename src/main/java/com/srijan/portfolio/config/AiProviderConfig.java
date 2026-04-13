package com.srijan.portfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AiProviderConfig {

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${GROQ_API_KEY:}")
    private String groqApiKey;

    @Value("${AI_TIMEOUT_MS:60000}")
    private int timeoutMs;

    @Value("${AI_PROVIDER_PRIORITY:gemini,groq}")
    private String providerPriority;

    @Value("${AI_PROVIDER_RETRY_ATTEMPTS:2}")
    private int maxRetryAttempts;

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
