package com.srijan.portfolio.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeParseResponseDto {

    // Identity
    private String name;
    private String email;
    private String phone;
    private String location;
    private String headline; // roleTitle
    private String summary; // short bio

    // About
    private List<String> about; // long-form about paragraphs
    private String availability;
    private String experienceYears;

    // Skills
    private List<String> skills;

    // Experience
    private List<ParsedExperience> experience;

    // Education
    private List<ParsedEducation> education;

    // Projects
    private List<ParsedProject> projects;

    // Certifications
    private List<String> certifications;

    // Social Links
    private String linkedinUrl;
    private String githubUrl;
    private String websiteUrl;
    private List<ParsedLink> otherLinks;

    // Principles (auto-generated if possible)
    private List<ParsedPrinciple> principles;

    // Meta
    private String provider; // "gemini", "groq", "internal"

    // ─── Nested types ────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedExperience {
        private String company;
        private String roleTitle;
        private String duration;
        private Integer startMonth;
        private Integer startYear;
        private Integer endMonth;
        private Integer endYear;
        private boolean current;
        private List<String> responsibilities;
        private List<String> achievements;
        private List<String> skills;
        private String location;

        // Education-type fields (for academic experience)
        private boolean academic;
        private String level;
        private String institute;
        private String degree;
        private String scoreLabel;
        private String scoreValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedEducation {
        private String level;
        private String institute;
        private String location;
        private String degree;
        private String scoreLabel;
        private String scoreValue;
        private String duration;
        private Integer startYear;
        private Integer endYear;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedProject {
        private String name;
        private String type;
        private String status;
        private String year;
        private String overview;
        private List<String> techStack;
        private String liveLink;
        private String sourceLink;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedLink {
        private String label;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedPrinciple {
        private String title;
        private String description;
    }
}
