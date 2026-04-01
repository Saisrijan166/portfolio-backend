package com.srijan.portfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class AiProviderConfig {

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${GROQ_API_KEY:}")
    private String groqApiKey;

    @Value("${AI_TIMEOUT_MS:8000}")
    private int timeoutMs;

    @Value("${RESUME_MAX_SIZE_BYTES:2097152}")
    private long maxFileSize; // 2MB default

    public boolean isGeminiAvailable() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    public boolean isGroqAvailable() {
        return groqApiKey != null && !groqApiKey.isBlank();
    }
}
