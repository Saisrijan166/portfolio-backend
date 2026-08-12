package com.srijan.portfolio.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResumePromptFactory {

    private final ObjectMapper objectMapper;

    public ResumePromptFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildResumeExtractionPrompt() {
        return """
                Parse the provided resume into JSON only.
                Return ONLY valid JSON. No markdown. No code fences. No explanation.
                Grounded in the provided resume, reasonably infer and generate missing fields (like descriptions, roles, achievements, etc.) accurately from the available context to ensure professional completeness.
                DO NOT add extra fields.
                If a value is missing and cannot be reasonably inferred, return null for scalars and [] for arrays.
                Arrays must never be null.
                Note for skills: For 'metaSkill' (identify from skills), set it to true (boolean) for soft skills, interpersonal skills, meta skills likewise. Otherwise false. If true, provide context for the skill in 'metaDescription' in exactly 1 sentence.
                %s
                Use this exact schema:
                {
                  "name": "",
                  "email": "",
                  "phone": "",
                  "location": "",
                  "headline": "",
                  "summary": "",
                  "about": [],
                  "availability": "",
                  "experienceYears": "",
                  "skills": [
                    {
                      "name": "",
                      "domain": "",
                      "metaSkill": false,
                      "metaDescription": ""
                    }
                  ],
                  "education": [
                    {
                      "level": "",
                      "institute": "",
                      "location": "",
                      "degree": "",
                      "scoreLabel": "",
                      "scoreValue": "",
                      "duration": "",
                      "startYear": 2020,
                      "endYear": 2024
                    }
                  ],
                  "experience": [
                    {
                      "company": "",
                      "roleTitle": "",
                      "duration": "",
                      "startMonth": 1,
                      "startYear": 2020,
                      "endMonth": 12,
                      "endYear": 2022,
                      "current": false,
                      "responsibilities": [],
                      "achievements": [],
                      "skills": [],
                      "location": ""
                    }
                  ],
                  "projects": [
                    {
                      "name": "",
                      "type": "",
                      "status": "",
                      "year": "",
                      "overview": "",
                      "techStack": [],
                      "liveLink": "",
                      "sourceLink": ""
                    }
                  ],
                  "certificationAchievements": [
                    {
                      "type": "",
                      "title": "",
                      "issuer": "",
                      "issuedOn": "",
                      "description": "",
                      "referenceUrl": "",
                      "imageUrl": ""
                    }
                  ],
                  "linkedinUrl": "",
                  "githubUrl": "",
                  "websiteUrl": "",
                  "otherLinks": [
                    {
                      "label": "",
                      "url": ""
                    }
                  ],
                  "principles": [
                    {
                      "title": "",
                      "description": ""
                    }
                  ]
                }
                """.formatted(buildLimitRules());
    }

    /**
     * The limits live in the system prompt for regeneration: the user prompt carries the existing
     * resume JSON and is length-capped before being sent, so rules placed there could be cut off.
     */
    public String buildSectionRegenerationSystemPrompt() {
        return "You improve resume sections while preserving facts." + buildLimitRules();
    }

    public String buildSectionRegenerationPrompt(List<String> sections, ResumeParseResponseDto existingResume) {
        if (existingResume == null) {
            throw new IllegalArgumentException("Existing resume is required for section regeneration");
        }
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("At least one section is required for section regeneration");
        }

        List<String> sanitizedSections = sections.stream()
                .filter(section -> section != null && !section.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (sanitizedSections.isEmpty()) {
            throw new IllegalArgumentException("At least one non-empty section is required for section regeneration");
        }

        return """
                Improve ONLY the requested sections of the existing parsed resume.
                Return ONLY valid JSON.
                DO NOT add extra fields.
                DO NOT overwrite sections that were not requested.
                Grounded in the provided resume data, reasonably infer and generate missing fields accurately from context.
                Requested sections: %s
                Existing parsed resume JSON:
                %s
                Return a partial JSON object containing only these top-level keys when requested:
                skills, experience, education, projects, certificationAchievements, summary, about, principles, headline.
                Arrays must never be null.
                Note for skills: If improving 'skills', for 'metaSkill' (identify from skills), set it to true (boolean) for soft skills, interpersonal skills, meta skills likewise. Otherwise false. If true, provide context for the skill in 'metaDescription' in exactly 1 sentence.
                Respect every limit in the system instructions. An existing value that is already over its limit must come back rewritten shorter, never returned as-is.
                """.formatted(String.join(", ", sanitizedSections), writeJson(existingResume));
    }

    public String buildResumeScoringPrompt(ResumeParseResponseDto resume) {
        if (resume == null) {
            throw new IllegalArgumentException("Resume is required for scoring");
        }
        return """
                Score this resume from 0 to 100 for professional clarity, quantified impact, completeness, and action-oriented writing.
                Return ONLY valid JSON with this exact schema:
                {
                  "score": 78,
                  "suggestions": [
                    "Add more quantified achievements",
                    "Improve action verbs"
                  ]
                }
                DO NOT add extra fields.
                Resume JSON:
                %s
                """.formatted(writeJson(resume));
    }

    /**
     * The output limits, rendered from {@link ResumeFieldLimits} so the prompt and
     * {@link ResumeLimitEnforcer} can never drift apart.
     *
     * <p>Values above these limits are rejected by the admin UI and the persisted DTOs, which is
     * why the model is told to compress rather than overflow: the facts must stay accurate, only
     * the wording gets tighter.
     */
    private String buildLimitRules() {
        return String.join("\n",
                "",
                "HARD OUTPUT LIMITS — these are absolute and must never be exceeded:",
                "Every character count below is a maximum for that single field. Every item count is a maximum for that single array.",
                "Write each value to FIT the limit: keep the facts accurate and complete, and compress the wording (shorter phrasing, drop filler words, no repetition) until it fits.",
                "Never exceed a limit and never cut a value off mid-word or mid-sentence — rewrite it shorter instead.",
                "If a list has more real items than its limit allows, keep the most significant ones and drop the rest.",
                "",
                "Identity:",
                "- name: max " + ResumeFieldLimits.NAME + " characters. Required, non-empty.",
                "- email: max " + ResumeFieldLimits.EMAIL + " characters, must be a valid email address.",
                "- phone: max " + ResumeFieldLimits.PHONE + " characters.",
                "- headline: max " + ResumeFieldLimits.HEADLINE + " characters. Required, non-empty. A single role title, not a sentence.",
                "- location: max " + ResumeFieldLimits.LOCATION + " characters, but aim for " + ResumeFieldLimits.LOCATION_TARGET
                        + ". Use a short \"City, Country\" form.",
                "- availability: max " + ResumeFieldLimits.AVAILABILITY + " characters, but aim for " + ResumeFieldLimits.AVAILABILITY_TARGET
                        + ". A short status such as \"Open to work\".",
                "- experienceYears: max " + ResumeFieldLimits.EXPERIENCE_YEARS + " characters, but aim for "
                        + ResumeFieldLimits.EXPERIENCE_YEARS_TARGET + ". A short form such as \"5+ years\".",
                "",
                "About:",
                "- summary: max " + ResumeFieldLimits.SUMMARY + " characters. A short professional bio.",
                "- about: max " + ResumeFieldLimits.ABOUT_MAX_PARAGRAPHS + " paragraphs, and the paragraphs COMBINED must be at most "
                        + ResumeFieldLimits.ABOUT_TOTAL + " characters in total. Prefer 2-4 paragraphs of up to "
                        + ResumeFieldLimits.ABOUT_PARAGRAPH_TARGET + " characters each.",
                "- principles: max " + ResumeFieldLimits.PRINCIPLES_MAX + " items. Each item requires a non-empty title (max "
                        + ResumeFieldLimits.PRINCIPLE_TITLE + " characters) and a non-empty description (max "
                        + ResumeFieldLimits.PRINCIPLE_DESCRIPTION + " characters).",
                "",
                "Skills — each skill:",
                "- name: max " + ResumeFieldLimits.SKILL_NAME + " characters. Required, non-empty. A short skill label, not a phrase.",
                "- domain: max " + ResumeFieldLimits.SKILL_DOMAIN + " characters.",
                "- metaDescription: max " + ResumeFieldLimits.SKILL_META_DESCRIPTION + " characters, exactly 1 short sentence.",
                "",
                "Experience — each entry:",
                "- company: max " + ResumeFieldLimits.EXPERIENCE_COMPANY + " characters.",
                "- roleTitle: max " + ResumeFieldLimits.EXPERIENCE_ROLE_TITLE + " characters.",
                "- duration: max " + ResumeFieldLimits.EXPERIENCE_DURATION + " characters, e.g. \"2021 - 2024\" or \"2022 - Present\".",
                "- location: max " + ResumeFieldLimits.EXPERIENCE_LOCATION + " characters.",
                "- responsibilities: max " + ResumeFieldLimits.RESPONSIBILITIES_MAX + " items, each at most "
                        + ResumeFieldLimits.RESPONSIBILITY + " characters. One concise bullet per item.",
                "- achievements: max " + ResumeFieldLimits.ACHIEVEMENTS_MAX + " items, each at most "
                        + ResumeFieldLimits.ACHIEVEMENT + " characters. One concise bullet per item.",
                "- skills: max " + ResumeFieldLimits.EXPERIENCE_SKILLS_MAX + " items, each at most "
                        + ResumeFieldLimits.EXPERIENCE_SKILL + " characters. Short tags only, never phrases or sentences.",
                "- startMonth / endMonth: integer " + ResumeFieldLimits.MIN_MONTH + "-" + ResumeFieldLimits.MAX_MONTH + " or null.",
                "- startYear / endYear: integer " + ResumeFieldLimits.MIN_YEAR + "-" + ResumeFieldLimits.MAX_YEAR + " or null.",
                "",
                "Education — each entry (level, institute, degree and duration are required and must be non-empty):",
                "- level: max " + ResumeFieldLimits.EDUCATION_LEVEL
                        + " characters. A short label such as \"Bachelors\", \"Masters\", \"Class 12\".",
                "- institute: max " + ResumeFieldLimits.EDUCATION_INSTITUTE + " characters.",
                "- location: max " + ResumeFieldLimits.EDUCATION_LOCATION + " characters.",
                "- degree: max " + ResumeFieldLimits.EDUCATION_DEGREE + " characters.",
                "- scoreLabel: max " + ResumeFieldLimits.EDUCATION_SCORE_LABEL + " characters, e.g. \"CGPA\" or \"Percentage\".",
                "- scoreValue: max " + ResumeFieldLimits.EDUCATION_SCORE_VALUE + " characters, e.g. \"8.6\" or \"92%\".",
                "- duration: max " + ResumeFieldLimits.EDUCATION_DURATION + " characters, e.g. \"2018 - 2022\".",
                "- startYear / endYear: integer " + ResumeFieldLimits.MIN_YEAR + "-" + ResumeFieldLimits.MAX_YEAR + " or null.",
                "",
                "Projects — each entry (name and overview are required and must be non-empty):",
                "- name: max " + ResumeFieldLimits.PROJECT_NAME + " characters.",
                "- type: max " + ResumeFieldLimits.PROJECT_TYPE + " characters, e.g. \"Personal\" or \"Client\".",
                "- status: max " + ResumeFieldLimits.PROJECT_STATUS + " characters, e.g. \"Completed\" or \"In progress\".",
                "- year: max " + ResumeFieldLimits.PROJECT_YEAR + " characters, but use a plain "
                        + ResumeFieldLimits.PROJECT_YEAR_TARGET + "-digit year such as \"2024\".",
                "- overview: max " + ResumeFieldLimits.PROJECT_OVERVIEW + " characters.",
                "- techStack: max " + ResumeFieldLimits.TECH_STACK_MAX + " items, each at most "
                        + ResumeFieldLimits.TECH_STACK_ITEM + " characters. Short technology tags only.",
                "- liveLink / sourceLink: max " + ResumeFieldLimits.URL
                        + " characters and must start with http:// or https://, otherwise return null.",
                "",
                "Certifications and achievements — max " + ResumeFieldLimits.CERTIFICATIONS_MAX
                        + " entries in total (type and title are required and must be non-empty):",
                "- type: exactly \"certification\" or \"achievement\", lowercase. No other value is accepted.",
                "- title: max " + ResumeFieldLimits.CERTIFICATION_TITLE + " characters.",
                "- issuer: max " + ResumeFieldLimits.CERTIFICATION_ISSUER + " characters, but aim for "
                        + ResumeFieldLimits.CERTIFICATION_ISSUER_TARGET + ".",
                "- issuedOn: an ISO date \"yyyy-MM-dd\", or null when the date is unknown. Never free text.",
                "- description: max " + ResumeFieldLimits.CERTIFICATION_DESCRIPTION + " characters.",
                "- referenceUrl / imageUrl: max " + ResumeFieldLimits.URL
                        + " characters and must start with http:// or https://, otherwise return null.",
                "",
                "Links:",
                "- linkedinUrl / githubUrl / websiteUrl: max " + ResumeFieldLimits.URL
                        + " characters and must start with http:// or https://, otherwise return null.",
                "- otherLinks: max " + ResumeFieldLimits.OTHER_LINKS_MAX + " items. Each item requires a non-empty label (max "
                        + ResumeFieldLimits.LINK_LABEL + " characters) and a non-empty url (max " + ResumeFieldLimits.URL
                        + " characters) starting with http://, https:// or mailto:.",
                "");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize prompt payload", exception);
        }
    }
}
