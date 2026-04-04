package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PortfolioResponse {
    private String username;
    private PortfolioIdentityDto profile;
    private PortfolioAboutSummaryDto about;
    private DesktopWidgetsDto widgets;
    private List<ProjectDto> projects;
    private List<ExperienceDto> experiences;
    private List<CertificationAchievementDto> certificationAchievements;
    private List<SkillDto> skills;
    private List<EducationDto> educations;
    private ResumeDto resume;
    private ContactDto contact;
    private String lastUpdated;
    // For public bootstrap endpoint
    private Long projectCount;
    private Long experienceCount;
}
