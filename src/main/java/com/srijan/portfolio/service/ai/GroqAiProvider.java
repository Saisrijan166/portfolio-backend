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

    public GroqAiProvider(AiProviderConfig config, ObjectMapper objectMapper) {
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
            requestJson = buildGroqRequest(extractedText);
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
        return "You are an expert resume parser with 100% accuracy. The user will provide raw text extracted from a resume document. "
                + "Extract ALL information with extreme precision and return a perfectly structured JSON object. "
                + ""
                + "CRITICAL RULES: "
                + "1. Return ONLY a single valid JSON object - absolutely NO markdown formatting, NO code blocks (no ```json), NO explanatory text before or after. "
                + "2. Extract data EXACTLY as written in the source text - do not rephrase, reformat, or interpret unless explicitly instructed. "
                + "3. Use null for genuinely missing data, never use empty strings or placeholder text like 'N/A' or 'Not specified'. "
                + "4. Use empty arrays [] only when the section conceptually exists but contains no items. "
                + "5. For dates: parse all formats (Jan 2020, 01/2020, January 2020, 2020-01, Jan '20) into numeric months (1-12 where January=1). "
                + "6. For current positions: if text contains 'Present', 'Current', 'Now', 'Till date', or shows no end date with recent start, set current=true and endMonth/endYear=null. "
                + "7. Preserve all original capitalization, spelling, company names, and job titles exactly as they appear. "
                + ""
                + "REQUIRED JSON SCHEMA (match this structure exactly): "
                + "{ "
                + "\"name\": string or null, "
                + "\"email\": string or null (extract valid email addresses only, not email-like patterns), "
                + "\"phone\": string or null (include country code if present, preserve original formatting with spaces/dashes), "
                + "\"location\": string or null (extract city, state/region, country as written - maintain original format), "
                + "\"headline\": string or null (exact current job title OR explicitly stated target role - do not infer or create), "
                + "\"summary\": string or null (extract professional summary/objective if present, max 500 chars; if missing, generate ONE concise sentence highlighting key strengths, experience level, and primary expertise based on work history), "
                + "\"about\": [string] or [] (extract 'About Me', 'Profile', or 'Bio' sections as separate paragraphs; if missing, generate 2-3 professional paragraphs covering: 1) background and experience overview, 2) key skills and expertise areas, 3) professional goals or value proposition - each max 500 chars), "
                + "\"availability\": string or null (extract ONLY explicit availability statements such as 'Available immediately', 'Notice period: 30 days', '2 weeks notice' - do not infer), "
                + "\"experienceYears\": string or null (extract if explicitly written like '5+ years', '8 years of experience'; otherwise calculate total years from earliest start date to latest end date or present, format as 'X+ Years' where X is rounded down to whole number), "
                + "\"skills\": [{\"name\": string (exact skill name as written, preserve casing), \"domain\": string (categorize into specific domains: Frontend, Backend, Database, DevOps, Cloud, Mobile, Design, Testing, Tools, Languages, Frameworks, AI/ML, Security, Data Science, Analytics, Product, or Other - be specific, avoid generic categories), \"metaSkill\": boolean (set to true ONLY for pure soft/cognitive skills: Leadership, Communication, Problem Solving, Critical Thinking, Teamwork, Time Management, Adaptability, Creativity, Emotional Intelligence - NEVER true for technical skills regardless of transferability), \"metaDescription\": string or null (populate ONLY when metaSkill=true, provide brief evidence or context of how this skill is demonstrated in their experience)}] or [], "
                + "\"experience\": [{\"company\": string (exact company/organization name), \"roleTitle\": string (exact job title/position), \"duration\": string (preserve original format: 'Jan 2020 - Present', '2020 - 2023', 'Jan 2020 - Dec 2022'), \"startMonth\": number 1-12 or null, \"startYear\": number (4-digit year) or null, \"endMonth\": number 1-12 or null, \"endYear\": number (4-digit year) or null, \"current\": boolean (true if currently employed in this role), \"responsibilities\": [string] (extract bullet points describing day-to-day duties, tasks, and responsibilities), \"achievements\": [string] (extract separately: quantified results, metrics, awards, promotions, recognitions, cost savings, revenue impact, efficiency improvements - anything showing measurable impact), \"skills\": [string] (extract specific technologies, tools, programming languages, frameworks mentioned in context of this role), \"location\": string or null (job location: city, state, country, or 'Remote')}] or [], "
                + "\"education\": [{\"level\": string (standardize to: 'High School', 'Associate', 'Bachelors', 'Masters', 'PhD', 'Diploma', 'Certificate', 'Professional Course' - if format differs, extract exact level), \"institute\": string (exact institution/university name), \"location\": string or null (institution location if specified), \"degree\": string (full degree name: 'Bachelor of Technology in Computer Science', 'MBA in Finance', 'B.Sc. Physics'), \"scoreLabel\": string or null (extract exact label: 'GPA', 'CGPA', 'Percentage', 'Grade Point', 'Marks', 'Class', 'Division'), \"scoreValue\": string or null (preserve exact format: '3.8/4.0', '8.5/10', '85%', 'First Class with Distinction'), \"duration\": string or null (original format: '2016-2020', '2016 to 2020'), \"startYear\": number (4-digit) or null, \"endYear\": number (4-digit) or null}] or [], "
                + "\"projects\": [{\"name\": string (exact project title/name), \"type\": string (extract if stated: 'Personal', 'Academic', 'Professional', 'Open Source', 'Freelance', 'Client', 'Research'; default to 'Personal' if not specified), \"status\": string (extract if stated: 'Completed', 'In Progress', 'Ongoing', 'Live'; default to 'Completed' if not specified), \"year\": string or null (extract project year, date range, or completion date if mentioned), \"overview\": string (extract existing project description; if description is missing or very brief, generate ONE concise sentence explaining what the project does and its key value/impact using project name and tech stack as context - max 200 chars, make it specific and technical), \"techStack\": [string] (extract ALL mentioned technologies, languages, frameworks, libraries, databases, platforms, tools), \"liveLink\": string or null (extract deployed application/demo URL if present), \"sourceLink\": string or null (extract GitHub, GitLab, Bitbucket, or code repository URL if present)}] or [], "
                + "\"certificationAchievements\": [{\"type\": string (use 'certification' for: professional certifications, licenses, course completions, technical certifications, credentials; use 'achievement' for: awards, honors, scholarships, competition wins, rankings, recognitions, publications, patents), \"title\": string (exact certification or achievement name), \"issuer\": string or null (issuing organization, platform, institution, or competition organizer), \"issuedOn\": string or null (preserve date format as written: 'January 2023', 'Jan 2023', '2023-01'), \"description\": string or null (extract any additional details, score, ranking, or context), \"referenceUrl\": string or null (credential verification link, certificate URL, competition page), \"imageUrl\": string or null (badge image URL or certificate scan URL if present)}] or [], "
                + "\"linkedinUrl\": string or null (complete LinkedIn profile URL starting with https://), "
                + "\"githubUrl\": string or null (complete GitHub profile URL starting with https://), "
                + "\"websiteUrl\": string or null (personal website, portfolio, or blog URL starting with https://), "
                + "\"otherLinks\": [{\"label\": string (platform or link description: 'Twitter', 'Stack Overflow', 'Medium', 'Behance', 'Dribbble', 'LeetCode', etc), \"url\": string (complete URL starting with https://)}] or [] (extract ALL other social media, professional platform, and portfolio links), "
                + "\"principles\": [{\"title\": string (concise principle name: 2-5 words), \"description\": string (1-2 sentences explaining the principle)}] or [] (generate 2-3 authentic professional principles or working values based on evidence from their resume: career progression patterns, project types, skills focus areas, achievements, and stated objectives - make principles specific and credible to this person's actual career path, not generic motivational statements) "
                + "}. "
                + ""
                + "FINAL VALIDATION CHECKLIST: "
                + "- All month values are integers 1-12 (January=1, December=12, never 0, never >12) "
                + "- All year values are 4-digit integers (2020, 2021, not 20, 21) "
                + "- Current positions have current=true AND endMonth=null AND endYear=null "
                + "- All URLs are complete and valid (start with http:// or https://) "
                + "- metaSkill=true appears ONLY on soft skills (Leadership, Communication, etc), NEVER on technical skills (Java, React, AWS, etc) "
                + "- metaDescription is populated ONLY when metaSkill=true, otherwise null "
                + "- JSON is valid: all strings properly escaped, all brackets matched, all commas correct "
                + "- Response starts with { and ends with } with absolutely NO additional text, markdown, or code block formatting "
                + "- No placeholder values like 'TBD', 'N/A', 'Not specified' - use null instead";
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
