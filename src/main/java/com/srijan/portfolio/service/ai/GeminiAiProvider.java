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
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Gemini 2.0 Flash — primary AI provider.
 * Sends the file as base64 inline for multimodal parsing.
 */
@Component
public class GeminiAiProvider implements AiProvider {

  private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);
  private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

  private final AiProviderConfig config;
  private final ObjectMapper objectMapper;
  private final OkHttpClient httpClient;
  private final ResumePromptFactory resumePromptFactory;

  public GeminiAiProvider(AiProviderConfig config, ObjectMapper objectMapper, ResumePromptFactory resumePromptFactory) {
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
    return "gemini";
  }

  @Override
  public boolean isAvailable() {
    return config.isGeminiAvailable();
  }

  @Override
  public ResumeParseResponseDto parseResume(byte[] fileBytes, String fileType, String extractedText)
      throws IOException {
    String base64Data = Base64.getEncoder().encodeToString(fileBytes);
    String mimeType = resolveMimeType(fileType);

    String requestJson = buildGeminiRequest(base64Data, mimeType, resumePromptFactory.buildResumeExtractionPrompt());

    Request request = new Request.Builder()
        .url(GEMINI_URL + "?key=" + config.getGeminiApiKey())
        .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Gemini API error: HTTP " + response.code());
      }

      ResponseBody body = response.body();
      if (body == null) {
        throw new IOException("Gemini returned empty response body");
      }

      return parseGeminiResponse(body.string());
    }
  }

  @Override
  public String generateJson(String systemPrompt, String userPrompt) throws IOException {
    String requestJson = """
        {
          "contents": [{
            "parts": [
              {"text": %s},
              {"text": %s}
            ]
          }],
          "generationConfig": {
            "temperature": 0.1,
            "responseMimeType": "application/json"
          }
        }
        """.formatted(
            objectMapper.writeValueAsString(systemPrompt),
            objectMapper.writeValueAsString(userPrompt)
    );

    Request request = new Request.Builder()
        .url(GEMINI_URL + "?key=" + config.getGeminiApiKey())
        .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Gemini API error: HTTP " + response.code());
      }
      ResponseBody body = response.body();
      if (body == null) {
        throw new IOException("Gemini returned empty response body");
      }
      JsonNode root = objectMapper.readTree(body.string());
      JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
      String jsonText = parts.path(0).path("text").asText("");
      if (jsonText.isBlank()) {
        throw new IOException("Gemini returned empty text content");
      }
      return stripMarkdownCodeFence(jsonText);
    }
  }

  private String buildGeminiRequest(String base64Data, String mimeType, String prompt) {
    return """
        {
          "contents": [{
            "parts": [
              {
                "inline_data": {
                  "mime_type": "%s",
                  "data": "%s"
                }
              },
              {
                "text": "%s"
              }
            ]
          }],
          "generationConfig": {
            "temperature": 0.1,
            "responseMimeType": "application/json"
          }
        }
        """.formatted(mimeType, base64Data, prompt);
  }

  private ResumeParseResponseDto parseGeminiResponse(String responseBody) throws IOException {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode candidates = root.path("candidates");

      if (!candidates.isArray() || candidates.isEmpty()) {
        throw new IOException("Gemini returned no candidates");
      }

      JsonNode parts = candidates.get(0).path("content").path("parts");
      if (!parts.isArray() || parts.isEmpty()) {
        throw new IOException("Gemini response has no content parts");
      }

      String jsonText = parts.get(0).path("text").asText("");
      if (jsonText.isBlank()) {
        throw new IOException("Gemini returned empty text content");
      }

      // Clean up in case Gemini wraps in markdown
      jsonText = stripMarkdownCodeFence(jsonText);

      ResumeParseResponseDto result = objectMapper.readValue(jsonText, ResumeParseResponseDto.class);
      result.setProvider("gemini");
      return result;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Failed to parse Gemini response: " + e.getMessage(), e);
    }
  }

  private String resolveMimeType(String fileType) throws IOException {
    if (fileType == null || fileType.isBlank()) {
      throw new IOException("Missing file type for Gemini resume parsing");
    }

    return switch (fileType.toLowerCase().trim()) {
      case "pdf" -> "application/pdf";
      case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      default -> throw new IOException("Unsupported file type for Gemini resume parsing: " + fileType);
    };
  }

  private String stripMarkdownCodeFence(String value) {
    return value
        .replaceFirst("(?s)^```(?:json)?\\s*", "")
        .replaceFirst("(?s)\\s*```\\s*$", "")
        .trim();
  }
}
