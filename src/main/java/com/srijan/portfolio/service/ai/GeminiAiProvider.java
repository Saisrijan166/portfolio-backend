package com.srijan.portfolio.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
  private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

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

  /**
   * The model name is part of the path, so a retired model answers 404 rather than failing at the
   * body. Keeping it in configuration means that is recoverable without a redeploy.
   */
  private String endpoint() {
    return GEMINI_BASE_URL + config.getGeminiModel() + ":generateContent?key=" + config.getGeminiApiKey();
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
        .url(endpoint())
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
    return generateContent(systemPrompt, userPrompt, true);
  }

  @Override
  public String generateText(String systemPrompt, String userPrompt) throws IOException {
    return generateContent(systemPrompt, userPrompt, false);
  }

  private String generateContent(String systemPrompt, String userPrompt, boolean jsonResponse) throws IOException {
    String requestJson = """
        {
          "contents": [{
            "parts": [
              {"text": %s},
              {"text": %s}
            ]
          }],
          "generationConfig": {
            "temperature": 0.1%s
          }
        }
        """.formatted(
            objectMapper.writeValueAsString(systemPrompt),
            objectMapper.writeValueAsString(userPrompt),
            jsonResponse ? ",\n            \"responseMimeType\": \"application/json\"" : ""
    );

    Request request = new Request.Builder()
        .url(endpoint())
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
      String text = parts.path(0).path("text").asText("");
      if (text.isBlank()) {
        throw new IOException("Gemini returned empty text content");
      }
      return stripMarkdownCodeFence(text);
    }
  }

  /**
   * Built through Jackson rather than string interpolation. The extraction prompt is a multi-line
   * block containing the literal JSON schema, so interpolating it into a quoted field produced a
   * body that was not valid JSON and the API rejected every request with HTTP 400.
   */
  private String buildGeminiRequest(String base64Data, String mimeType, String prompt)
      throws JsonProcessingException {
    ObjectNode root = objectMapper.createObjectNode();

    ObjectNode parts0 = objectMapper.createObjectNode();
    ObjectNode inlineData = parts0.putObject("inline_data");
    inlineData.put("mime_type", mimeType);
    inlineData.put("data", base64Data);

    ObjectNode parts1 = objectMapper.createObjectNode();
    parts1.put("text", prompt);

    ObjectNode content = objectMapper.createObjectNode();
    content.putArray("parts").add(parts0).add(parts1);
    root.putArray("contents").add(content);

    ObjectNode generationConfig = root.putObject("generationConfig");
    generationConfig.put("temperature", 0.1);
    generationConfig.put("responseMimeType", "application/json");

    return objectMapper.writeValueAsString(root);
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
