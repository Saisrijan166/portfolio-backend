package com.srijan.portfolio.dto;

import jakarta.validation.Valid;
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

    @Valid
    private List<ProjectDto> projects;

    @Valid
    private List<ExperienceDto> experience;

    @Valid
    private List<EducationDto> education;

    @Valid
    private List<CertificationAchievementDto> certifications;

    @Valid
    private List<SkillDto> skills;
}
