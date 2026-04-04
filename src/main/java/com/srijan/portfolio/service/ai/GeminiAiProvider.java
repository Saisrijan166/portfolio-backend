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

  public GeminiAiProvider(AiProviderConfig config, ObjectMapper objectMapper) {
    this.config = config;
    this.objectMapper = objectMapper;
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

    String requestJson = buildGeminiRequest(base64Data, mimeType);

    Request request = new Request.Builder()
        .url(GEMINI_URL + "?key=" + config.getGeminiApiKey())
        .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.warn("Gemini returned HTTP {}", response.code());
        throw new IOException("Gemini API error: HTTP " + response.code());
      }

      ResponseBody body = response.body();
      if (body == null) {
        throw new IOException("Gemini returned empty response body");
      }

      return parseGeminiResponse(body.string());
    }
  }

  private String buildGeminiRequest(String base64Data, String mimeType) {
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
        """.formatted(mimeType, base64Data, getExtractionPrompt());
  }

  private String getExtractionPrompt() {
    return "You are a precise resume parser. Extract ALL information from this resume document and return ONLY a valid JSON object. "
        + "The JSON must have these exact fields (use null for missing data, empty arrays [] for missing lists): "
        + "{ "
        + "\\\"name\\\": string or null, "
        + "\\\"email\\\": string or null, "
        + "\\\"phone\\\": string or null, "
        + "\\\"location\\\": string or null, "
        + "\\\"headline\\\": string (current job title or desired role) or null, "
        + "\\\"summary\\\": string (short bio/objective, max 500 chars, generate concise one if not present) or null, "
        + "\\\"about\\\": [string] (detailed about paragraphs, generate if not present based on resume content, each max 500 chars) or [], "
        + "\\\"availability\\\": string or null, "
        + "\\\"experienceYears\\\": string (e.g. '3+ Years', calculate from work history if not stated) or null, "
        + "\\\"skills\\\": [string] (all technical and soft skills) or [], "
        + "\\\"experience\\\": [{\\\"company\\\": string, \\\"roleTitle\\\": string, \\\"duration\\\": string, \\\"startMonth\\\": number or null, \\\"startYear\\\": number or null, \\\"endMonth\\\": number or null, \\\"endYear\\\": number or null, \\\"current\\\": boolean, \\\"responsibilities\\\": [string], \\\"achievements\\\": [string], \\\"skills\\\": [string], \\\"location\\\": string or null}] or [], "
        + "\\\"education\\\": [{\\\"level\\\": string (e.g. 'Bachelors', 'Masters', 'PhD', 'High School'), \\\"institute\\\": string, \\\"location\\\": string or null, \\\"degree\\\": string, \\\"scoreLabel\\\": string or null (e.g. 'GPA', 'Percentage', 'CGPA'), \\\"scoreValue\\\": string or null, \\\"duration\\\": string or null, \\\"startYear\\\": number or null, \\\"endYear\\\": number or null}] or [], "
        + "\\\"projects\\\": [{\\\"name\\\": string, \\\"type\\\": string or 'Personal', \\\"status\\\": string or 'Completed', \\\"year\\\": string or null, \\\"overview\\\": string (generate concise description from name and tech if overview missing, max 200 chars), \\\"techStack\\\": [string], \\\"liveLink\\\": string or null, \\\"sourceLink\\\": string or null}] or [], "
        + "\\\"certifications\\\": [string] or [], "
        + "\\\"linkedinUrl\\\": string or null, "
        + "\\\"githubUrl\\\": string or null, "
        + "\\\"websiteUrl\\\": string or null, "
        + "\\\"otherLinks\\\": [{\\\"label\\\": string, \\\"url\\\": string}] or [], "
        + "\\\"principles\\\": [{\\\"title\\\": string, \\\"description\\\": string}] (generate 2-3 professional principles based on resume content) or [] "
        + "}. "
        + "IMPORTANT: Return ONLY the JSON object, no markdown, no code blocks, no explanation. "
        + "For generated fields (about, summary, principles, project overviews): create professional, accurate content based on the resume data. "
        + "For months use 1-12 (January=1). Parse all dates accurately. Mark current positions with current=true.";
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
