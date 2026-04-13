package com.srijan.portfolio.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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
                """.formatted(String.join(", ", sections), writeJson(existingResume));
    }

    public String buildResumeScoringPrompt(ResumeParseResponseDto resume) {
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
