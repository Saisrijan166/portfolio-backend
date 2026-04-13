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

        resume.setName(cleanString(resume.getName()));
        resume.setEmail(cleanString(resume.getEmail()));
        resume.setPhone(cleanString(resume.getPhone()));
        resume.setLocation(cleanString(resume.getLocation()));
        resume.setHeadline(cleanString(resume.getHeadline()));
        resume.setSummary(cleanString(resume.getSummary()));
        resume.setAvailability(cleanString(resume.getAvailability()));
        resume.setExperienceYears(cleanString(resume.getExperienceYears()));
        resume.setLinkedinUrl(cleanString(resume.getLinkedinUrl()));
        resume.setGithubUrl(cleanString(resume.getGithubUrl()));
        resume.setWebsiteUrl(cleanString(resume.getWebsiteUrl()));
        resume.setAbout(cleanStrings(resume.getAbout()));
        resume.setSkills(resume.getSkills() == null ? new ArrayList<>() : resume.getSkills());
        resume.setExperience(resume.getExperience() == null ? new ArrayList<>() : resume.getExperience());
        resume.setEducation(resume.getEducation() == null ? new ArrayList<>() : resume.getEducation());
        resume.setProjects(resume.getProjects() == null ? new ArrayList<>() : resume.getProjects());
        resume.setCertificationAchievements(resume.getCertificationAchievements() == null ? new ArrayList<>() : resume.getCertificationAchievements());
        resume.setOtherLinks(resume.getOtherLinks() == null ? new ArrayList<>() : resume.getOtherLinks());
        resume.setPrinciples(resume.getPrinciples() == null ? new ArrayList<>() : resume.getPrinciples());
        resume.setProvider(cleanString(resume.getProvider()));
        return resume;
    }

    public boolean isValid(ResumeParseResponseDto resume) {
        if (resume == null) {
            return false;
        }

        ResumeParseResponseDto cleaned = clean(resume);
        return cleaned.getSkills() != null
                && cleaned.getEducation() != null
                && cleaned.getExperience() != null
                && cleaned.getProjects() != null
                && cleaned.getCertificationAchievements() != null
                && cleaned.getOtherLinks() != null
                && cleaned.getPrinciples() != null;
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
                .toList();
    }
}
