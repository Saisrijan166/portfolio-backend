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
    private ProfileDto profile;
    private List<ProjectDto> projects;
    private List<ExperienceDto> experiences;
    private List<SkillDto> skills;
    private List<EducationDto> educations;
    private ResumeDto resume;
    private ContactDto contact;
}
