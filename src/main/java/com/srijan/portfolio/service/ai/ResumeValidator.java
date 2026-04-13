package com.srijan.portfolio.service.ai;

import com.srijan.portfolio.dto.ResumeParseResponseDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResumeValidator {

    public ResumeParseResponseDto clean(ResumeParseResponseDto resume) {
        if (resume == null) {
            return null;
        }

        return ResumeParseResponseDto.builder()
                .name(cleanString(resume.getName()))
                .email(cleanString(resume.getEmail()))
                .phone(cleanString(resume.getPhone()))
                .location(cleanString(resume.getLocation()))
                .headline(cleanString(resume.getHeadline()))
                .summary(cleanString(resume.getSummary()))
                .about(cleanStrings(resume.getAbout()))
                .availability(cleanString(resume.getAvailability()))
                .experienceYears(cleanString(resume.getExperienceYears()))
                .skills(resume.getSkills() == null ? new ArrayList<>() : new ArrayList<>(resume.getSkills()))
                .experience(resume.getExperience() == null ? new ArrayList<>() : new ArrayList<>(resume.getExperience()))
                .education(resume.getEducation() == null ? new ArrayList<>() : new ArrayList<>(resume.getEducation()))
                .projects(resume.getProjects() == null ? new ArrayList<>() : new ArrayList<>(resume.getProjects()))
                .certificationAchievements(resume.getCertificationAchievements() == null ? new ArrayList<>() : new ArrayList<>(resume.getCertificationAchievements()))
                .linkedinUrl(cleanString(resume.getLinkedinUrl()))
                .githubUrl(cleanString(resume.getGithubUrl()))
                .websiteUrl(cleanString(resume.getWebsiteUrl()))
                .otherLinks(resume.getOtherLinks() == null ? new ArrayList<>() : new ArrayList<>(resume.getOtherLinks()))
                .principles(resume.getPrinciples() == null ? new ArrayList<>() : new ArrayList<>(resume.getPrinciples()))
                .provider(cleanString(resume.getProvider()))
                .build();
    }

    public boolean isValid(ResumeParseResponseDto resume) {
        if (resume == null
                || resume.getSkills() == null
                || resume.getEducation() == null
                || resume.getExperience() == null
                || resume.getProjects() == null
                || resume.getCertificationAchievements() == null
                || resume.getOtherLinks() == null
                || resume.getPrinciples() == null) {
            return false;
        }

        return hasText(resume.getName())
                || hasText(resume.getEmail())
                || hasText(resume.getPhone())
                || hasText(resume.getHeadline())
                || !resume.getSkills().isEmpty()
                || !resume.getExperience().isEmpty()
                || !resume.getEducation().isEmpty();
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private List<String> cleanStrings(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .map(this::cleanString)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }
}
