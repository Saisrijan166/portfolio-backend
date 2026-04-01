package com.srijan.portfolio.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.config.AiProviderConfig;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    public GroqAiProvider(AiProviderConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
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
            throws Exception {
        if (extractedText == null || extractedText.isBlank()) {
            throw new RuntimeException("No extracted text available for Groq");
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();

        String requestJson = buildGroqRequest(extractedText);

        Request request = new Request.Builder()
                .url(GROQ_URL)
                .addHeader("Authorization", "Bearer " + config.getGroqApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("Groq returned HTTP {}", response.code());
                throw new RuntimeException("Groq API error: HTTP " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return parseGroqResponse(responseBody);
        }
    }

    private String buildGroqRequest(String resumeText) throws Exception {
        // Truncate very long texts to stay within token limits
        String text = resumeText.length() > 12000 ? resumeText.substring(0, 12000) : resumeText;

        String systemPrompt = getExtractionPrompt();

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

    private String getExtractionPrompt() {
        return "You are a precise resume parser. The user will provide raw text extracted from a resume document. "
                + "Extract ALL information and return ONLY a valid JSON object with these exact fields (use null for missing data, empty arrays [] for missing lists): "
                + "{ "
                + "\"name\": string or null, "
                + "\"email\": string or null, "
                + "\"phone\": string or null, "
                + "\"location\": string or null, "
                + "\"headline\": string (current job title or desired role) or null, "
                + "\"summary\": string (short bio/objective, max 500 chars, generate concise one if not present) or null, "
                + "\"about\": [string] (detailed about paragraphs, generate if not present, each max 500 chars) or [], "
                + "\"availability\": string or null, "
                + "\"experienceYears\": string (calculate from work history if not stated) or null, "
                + "\"skills\": [string] (all technical and soft skills) or [], "
                + "\"experience\": [{\"company\": string, \"roleTitle\": string, \"duration\": string, \"startMonth\": number or null, \"startYear\": number or null, \"endMonth\": number or null, \"endYear\": number or null, \"current\": boolean, \"responsibilities\": [string], \"achievements\": [string], \"skills\": [string], \"location\": string or null}] or [], "
                + "\"education\": [{\"level\": string, \"institute\": string, \"location\": string or null, \"degree\": string, \"scoreLabel\": string or null, \"scoreValue\": string or null, \"duration\": string or null, \"startYear\": number or null, \"endYear\": number or null}] or [], "
                + "\"projects\": [{\"name\": string, \"type\": string or \"Personal\", \"status\": string or \"Completed\", \"year\": string or null, \"overview\": string (generate if missing), \"techStack\": [string], \"liveLink\": string or null, \"sourceLink\": string or null}] or [], "
                + "\"certifications\": [string] or [], "
                + "\"linkedinUrl\": string or null, "
                + "\"githubUrl\": string or null, "
                + "\"websiteUrl\": string or null, "
                + "\"otherLinks\": [{\"label\": string, \"url\": string}] or [], "
                + "\"principles\": [{\"title\": string, \"description\": string}] (generate 2-3 based on resume) or [] "
                + "}. "
                + "Return ONLY the JSON object. Generate professional content for about, summary, principles, and project overviews when missing.";
    }

    private ResumeParseResponseDto parseGroqResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");

        if (choices.isEmpty() || !choices.isArray()) {
            throw new RuntimeException("Groq returned no choices");
        }

        String jsonText = choices.get(0).path("message").path("content").asText();

        // Clean up markdown wrapping if present
        jsonText = jsonText.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*$", "").trim();

        ResumeParseResponseDto result = objectMapper.readValue(jsonText, ResumeParseResponseDto.class);
        result.setProvider("groq");
        return result;
    }
}
