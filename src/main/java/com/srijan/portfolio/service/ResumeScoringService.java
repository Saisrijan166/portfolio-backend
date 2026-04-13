package com.srijan.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import com.srijan.portfolio.dto.ResumeScoreDto;
import com.srijan.portfolio.service.ai.AiOrchestratorService;
import com.srijan.portfolio.service.ai.ResumePromptFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeScoringService {

    private final AiOrchestratorService aiOrchestratorService;
    private final ResumePromptFactory resumePromptFactory;
    private final ObjectMapper objectMapper;

    public ResumeScoreDto score(ResumeParseResponseDto resume) {
        try {
            String rawJson = aiOrchestratorService.generateJson(
                    "You are a strict resume reviewer.",
                    resumePromptFactory.buildResumeScoringPrompt(resume),
                    this::isValidScoreResponse
            );

            JsonNode node = objectMapper.readTree(rawJson);
            List<String> suggestions = new ArrayList<>();
            node.path("suggestions").forEach(item -> suggestions.add(item.asText("")));

            return ResumeScoreDto.builder()
                    .score(Math.max(0, Math.min(100, node.path("score").asInt(0))))
                    .suggestions(suggestions.stream().filter(item -> !item.isBlank()).toList())
                    .provider("ai")
                    .build();
        } catch (Exception exception) {
            return heuristicScore(resume);
        }
    }

    private boolean isValidScoreResponse(String rawJson) {
        try {
            JsonNode node = objectMapper.readTree(rawJson);
            return node.has("score") && node.path("suggestions").isArray();
        } catch (Exception exception) {
            return false;
        }
    }

    private ResumeScoreDto heuristicScore(ResumeParseResponseDto resume) {
        int score = 40;
        List<String> suggestions = new ArrayList<>();
        if (resume.getSummary() != null && !resume.getSummary().isBlank()) {
            score += 10;
        } else {
            suggestions.add("Add a concise professional summary");
        }
        if (resume.getExperience() != null && !resume.getExperience().isEmpty()) {
            score += 20;
        } else {
            suggestions.add("Add work experience with concrete responsibilities");
        }
        boolean hasQuantifiedAchievement = resume.getExperience() != null && resume.getExperience().stream()
                .flatMap(item -> item.getAchievements() == null ? List.<String>of().stream() : item.getAchievements().stream())
                .anyMatch(item -> item.matches(".*\\d+.*"));
        if (hasQuantifiedAchievement) {
            score += 15;
        } else {
            suggestions.add("Add more quantified achievements");
        }
        if (resume.getSkills() != null && resume.getSkills().size() >= 5) {
            score += 10;
        } else {
            suggestions.add("Expand the technical skills section with core tools");
        }
        if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
            score += 5;
        } else {
            suggestions.add("Include at least one project or portfolio example");
        }

        return ResumeScoreDto.builder()
                .score(Math.min(100, score))
                .suggestions(suggestions.stream().limit(3).toList())
                .provider("heuristic")
                .build();
    }
}
