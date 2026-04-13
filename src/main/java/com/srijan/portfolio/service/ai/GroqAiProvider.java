package com.srijan.portfolio.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.config.AiProviderConfig;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Groq (Llama 3) — fallback AI provider.
 * Receives pre-extracted text only (Groq cannot read binary files).
 */
@Component
public class GroqAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GroqAiProvider.class);
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final AiProviderConfig config;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final ResumePromptFactory resumePromptFactory;

    public GroqAiProvider(AiProviderConfig config, ObjectMapper objectMapper, ResumePromptFactory resumePromptFactory) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.resumePromptFactory = resumePromptFactory;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public String getName() {
        return "groq";
    }

    @Override
    public boolean isAvailable() {
        return config.isGroqAvailable();
    }

    @Override
    public ResumeParseResponseDto parseResume(byte[] fileBytes, String fileType, String extractedText)
            throws IOException {
        if (extractedText == null || extractedText.isBlank()) {
            throw new IOException("No extracted text available for Groq");
        }

        String requestJson;
        try {
            requestJson = buildGroqRequest(resumePromptFactory.buildResumeExtractionPrompt(), extractedText);
        } catch (Exception e) {
            throw new IOException("Failed to build Groq request: " + e.getMessage(), e);
        }

        Request request = new Request.Builder()
                .url(GROQ_URL)
                .addHeader("Authorization", "Bearer " + config.getGroqApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("Groq returned HTTP {}", response.code());
                throw new IOException("Groq API error: HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Groq returned empty response body");
            }

            return parseGroqResponse(body.string());
        }
    }

    @Override
    public String generateJson(String systemPrompt, String userPrompt) throws IOException {
        try {
            Request request = new Request.Builder()
                    .url(GROQ_URL)
                    .addHeader("Authorization", "Bearer " + config.getGroqApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(buildGroqRequest(systemPrompt, userPrompt), MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Groq API error: HTTP " + response.code());
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new IOException("Groq returned empty response body");
                }
                JsonNode root = objectMapper.readTree(body.string());
                String jsonText = root.path("choices").path(0).path("message").path("content").asText("");
                if (jsonText.isBlank()) {
                    throw new IOException("Groq returned empty message content");
                }
                return stripMarkdownCodeFence(jsonText);
            }
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Failed to call Groq generation: " + exception.getMessage(), exception);
        }
    }

    private String buildGroqRequest(String systemPrompt, String userPrompt) throws Exception {
        // Truncate very long texts to stay within token limits
        String text = userPrompt.length() > 12000 ? userPrompt.substring(0, 12000) : userPrompt;

        // Build JSON manually to handle escaping properly
        String escapedText = objectMapper.writeValueAsString(text);
        String escapedSystem = objectMapper.writeValueAsString(systemPrompt);

        return """
                {
                  "model": "llama-3.3-70b-versatile",
                  "messages": [
                    {"role": "system", "content": %s},
                    {"role": "user", "content": %s}
                  ],
                  "temperature": 0.1,
                  "max_tokens": 4096,
                  "response_format": {"type": "json_object"}
                }
                """.formatted(escapedSystem, escapedText);
    }

    private ResumeParseResponseDto parseGroqResponse(String responseBody) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                throw new IOException("Groq returned no choices");
            }

            String jsonText = choices.get(0).path("message").path("content").asText("");
            if (jsonText.isBlank()) {
                throw new IOException("Groq returned empty message content");
            }

            // Clean up markdown wrapping if present
            jsonText = stripMarkdownCodeFence(jsonText);

            ResumeParseResponseDto result = objectMapper.readValue(jsonText, ResumeParseResponseDto.class);
            result.setProvider("groq");
            return result;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to parse Groq response: " + e.getMessage(), e);
        }
    }

    private String stripMarkdownCodeFence(String value) {
        return value
                .replaceFirst("(?s)^```(?:json)?\\s*", "")
                .replaceFirst("(?s)\\s*```\\s*$", "")
                .trim();
    }
}
