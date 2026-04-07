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
    return "You are an expert resume parser with 100% accuracy. Extract ALL information from this resume document with extreme precision. "
        + "CRITICAL RULES: "
        + "1. Return ONLY a single valid JSON object - absolutely NO markdown formatting, NO code blocks (no ```json), NO explanatory text before or after. "
        + "2. Extract data EXACTLY as written - do not rephrase, reformat, or interpret unless explicitly instructed. "
        + "3. Use null for genuinely missing data, never use empty strings or placeholder text. "
        + "4. Use empty arrays [] only when the section exists but is empty. "
        + "5. For dates: parse all formats (Jan 2020, 01/2020, January 2020, 2020-01) into numeric months (1-12 where January=1). "
        + "6. For current positions: if you see 'Present', 'Current', 'Now', or no end date with recent start, set current=true and endMonth/endYear=null. "
        + "7. Preserve all original capitalization, spelling, and formatting from the source document. "
        + ""
        + "REQUIRED JSON SCHEMA (match this structure exactly): "
        + "{ "
        + "\\\"name\\\": string or null, "
        + "\\\"email\\\": string or null (extract email addresses only, not email-like text), "
        + "\\\"phone\\\": string or null (include country code if present, preserve formatting), "
        + "\\\"location\\\": string or null (city, state/region, country - extract as written), "
        + "\\\"headline\\\": string or null (exact current job title OR explicitly stated desired role - do not infer), "
        + "\\\"summary\\\": string or null (extract professional summary/objective if present, max 500 chars; if missing, generate ONE concise sentence highlighting key experience and expertise based on work history and skills), "
        + "\\\"about\\\": [string] or [] (extract 'About Me' or similar sections as-is; if missing, generate 2-3 professional paragraphs from resume content covering background, expertise, and value proposition - each max 500 chars), "
        + "\\\"availability\\\": string or null (extract ONLY explicit availability statements like 'Available immediately', 'Notice period: 30 days'), "
        + "\\\"experienceYears\\\": string or null (extract if explicitly stated like '5+ years experience'; otherwise calculate from earliest to latest work experience and format as 'X+ Years' where X is rounded down), "
        + "\\\"skills\\\": [{\\\"name\\\": string (exact skill name as written), \\\"domain\\\": string (categorize into: Frontend, Backend, Database, DevOps, Cloud, Mobile, Design, Testing, Tools, Languages, Frameworks, AI/ML, Security, Data Science, or Other - NOT meta-categories like 'Technical'), \\\"metaSkill\\\": boolean (true ONLY for pure cognitive/soft skills: Leadership, Communication, Problem Solving, Critical Thinking, Teamwork, Time Management, Adaptability, Creativity - NOT for technical skills even if transferable), \\\"metaDescription\\\": string or null (populate ONLY when metaSkill=true, describe how this skill is demonstrated)}] or [], "
        + "\\\"experience\\\": [{\\\"company\\\": string (exact company name), \\\"roleTitle\\\": string (exact job title), \\\"duration\\\": string (preserve original format like 'Jan 2020 - Present' or '2020 - 2023'), \\\"startMonth\\\": number 1-12 or null, \\\"startYear\\\": number (4 digits) or null, \\\"endMonth\\\": number 1-12 or null, \\\"endYear\\\": number (4 digits) or null, \\\"current\\\": boolean (true if still employed here), \\\"responsibilities\\\": [string] (extract bullet points describing duties/responsibilities), \\\"achievements\\\": [string] (extract quantified accomplishments, metrics, awards, promotions separately from responsibilities), \\\"skills\\\": [string] (extract technologies/tools mentioned for this specific role), \\\"location\\\": string or null (job location if specified)}] or [], "
        + "\\\"education\\\": [{\\\"level\\\": string (standardize to: 'High School', 'Associate', 'Bachelors', 'Masters', 'PhD', 'Diploma', 'Certificate', or extract exact level if different), \\\"institute\\\": string (exact institution name), \\\"location\\\": string or null (institution location), \\\"degree\\\": string (exact degree/major name like 'B.Tech in Computer Science', 'MBA'), \\\"scoreLabel\\\": string or null (extract as written: 'GPA', 'CGPA', 'Percentage', 'Grade', 'Class'), \\\"scoreValue\\\": string or null (preserve exact format: '3.8/4.0', '85%', 'First Class'), \\\"duration\\\": string or null (original format like '2016-2020'), \\\"startYear\\\": number or null, \\\"endYear\\\": number or null}] or [], "
        + "\\\"projects\\\": [{\\\"name\\\": string (exact project name), \\\"type\\\": string (extract if stated: 'Personal', 'Academic', 'Professional', 'Open Source', 'Freelance'; default to 'Personal'), \\\"status\\\": string (extract if stated: 'Completed', 'In Progress', 'Ongoing'; default to 'Completed'), \\\"year\\\": string or null (extract year/date if mentioned), \\\"overview\\\": string (extract existing description; if missing, generate ONE concise sentence describing project purpose and impact using name and tech stack - max 200 chars), \\\"techStack\\\": [string] (extract ALL technologies/frameworks mentioned), \\\"liveLink\\\": string or null (extract live/demo URL if present), \\\"sourceLink\\\": string or null (extract GitHub/repository URL if present)}] or [], "
        + "\\\"certificationAchievements\\\": [{\\\"type\\\": string ('certification' for professional certifications/licenses, 'achievement' for awards/honors/recognitions/scholarships/competition wins/rankings), \\\"title\\\": string (exact name), \\\"issuer\\\": string or null (issuing organization), \\\"issuedOn\\\": string or null (preserve date format as written), \\\"description\\\": string or null (extract any additional details), \\\"referenceUrl\\\": string or null (verification/credential URL), \\\"imageUrl\\\": string or null (badge/certificate image URL if present)}] or [], "
        + "\\\"linkedinUrl\\\": string or null (full LinkedIn profile URL), "
        + "\\\"githubUrl\\\": string or null (full GitHub profile URL), "
        + "\\\"websiteUrl\\\": string or null (personal website/portfolio URL), "
        + "\\\"otherLinks\\\": [{\\\"label\\\": string (platform name or description), \\\"url\\\": string (full URL)}] or [] (extract any other social/professional links like Twitter, Stack Overflow, Medium, Behance), "
        + "\\\"principles\\\": [{\\\"title\\\": string, \\\"description\\\": string}] or [] (generate 2-3 professional principles/values based on work history, achievements, and skills - infer from career progression, project choices, and stated objectives - make them specific and authentic to this person's career narrative) "
        + "}. "
        + ""
        + "FINAL VALIDATION: "
        + "- Verify all months are 1-12 (not 0, not >12) "
        + "- Verify all years are 4-digit numbers (2015, not 15) "
        + "- Verify current positions have current=true and null end dates "
        + "- Verify all URLs are complete (include https://) "
        + "- Verify metaSkill=true ONLY for soft skills, never for technical skills "
        + "- Double-check JSON is valid and properly escaped "
        + "- Ensure response starts with { and ends with } with NO additional text";
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
