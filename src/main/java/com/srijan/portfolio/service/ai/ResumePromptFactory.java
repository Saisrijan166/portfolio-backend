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
                DO NOT hallucinate missing facts.
                DO NOT add extra fields.
                If a value is missing, return null for scalars and [] for arrays.
                Arrays must never be null.
                Note for skills: For 'metaSkill' (identify from skills), set it to true for soft skills, interpersonal skills, meta skills likewise. Otherwise false. If true, provide context related to the skill in 'metaDescription'.
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
                """;
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
                Keep facts grounded in the provided resume data only.
                Requested sections: %s
                Existing parsed resume JSON:
                %s
                Return a partial JSON object containing only these top-level keys when requested:
                skills, experience, education, projects, certificationAchievements, summary, about, principles, headline.
                Arrays must never be null.
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize prompt payload", exception);
        }
    }
}
