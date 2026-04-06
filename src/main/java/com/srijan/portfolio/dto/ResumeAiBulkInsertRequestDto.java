package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeAiBulkInsertRequestDto {

    @NotEmpty
    @Size(max = 10)
    private List<
            @Pattern(
                    regexp = "^(projects|experience|education|certifications|skills)$",
                    message = "Section must be one of: projects, experience, education, certifications, skills"
            ) String> sections;

    private List<ProjectDto> projects;
    private List<ExperienceDto> experience;
    private List<EducationDto> education;
    private List<CertificationAchievementDto> certifications;
    private List<SkillDto> skills;
}
