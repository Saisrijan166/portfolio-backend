package com.srijan.portfolio.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.config.AiProviderConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class SummarizeGroqClient {

    private static final Logger log = LoggerFactory.getLogger(SummarizeGroqClient.class);
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final AiProviderConfig config;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public SummarizeGroqClient(AiProviderConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    public String generateSummary(String systemPrompt, String userPrompt) {
        if (!config.isGroqAvailable()) {
            throw new IllegalStateException("Groq summarize API key is missing");
        }
        
        try {
            Request request = new Request.Builder()
                    .url(GROQ_URL)
                    .addHeader("Authorization", "Bearer " + config.getGroqApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(buildGroqTextRequest(systemPrompt, userPrompt), MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Groq API error for summarize: HTTP {}", response.code());
                    throw new RuntimeException("Groq API error: HTTP " + response.code() + " body: " + (response.body() != null ? response.body().string() : ""));
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new RuntimeException("Groq returned empty response body");
                }
                JsonNode root = objectMapper.readTree(body.string());
                String text = root.path("choices").path(0).path("message").path("content").asText("");
                if (text.isBlank()) {
                    throw new RuntimeException("Groq returned empty message content");
                }
                return stripMarkdownCodeFence(text);
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception exception) {
            log.error("Failed to generate summary with Groq: {}", exception.getMessage());
            throw new RuntimeException("Summarization failed.", exception);
        }
    }

    private String buildGroqTextRequest(String systemPrompt, String userPrompt) throws Exception {
        String text = userPrompt.length() > 12000 ? userPrompt.substring(0, 12000) : userPrompt;
        String escapedText = objectMapper.writeValueAsString(text);
        String escapedSystem = objectMapper.writeValueAsString(systemPrompt);

        return """
                {
                  "model": %s,
                  "messages": [
                    {"role": "system", "content": %s},
                    {"role": "user", "content": %s}
                  ],
                  "temperature": 0.3,
                  "max_tokens": 2048
                }
                """.formatted(
                        objectMapper.writeValueAsString(config.getGroqModel()),
                        escapedSystem,
                        escapedText);
    }

    private String stripMarkdownCodeFence(String value) {
        return value
                .replaceFirst("(?s)^```(?:json)?\\s*", "")
                .replaceFirst("(?s)\\s*```\\s*$", "")
                .trim();
    }
}
