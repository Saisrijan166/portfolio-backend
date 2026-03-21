package com.srijan.portfolio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class ExperienceDto {
    private Long id;

    @Size(max = 100)
    private String company;

    @Size(max = 100)
    private String roleTitle;

    @Size(max = 100)
    private String duration;

    @Min(1)
    @Max(12)
    private Integer startMonth;

    @Min(1950)
    @Max(2100)
    private Integer startYear;

    @Min(1)
    @Max(12)
    private Integer endMonth;

    @Min(1950)
    @Max(2100)
    private Integer endYear;

    private boolean isCurrent;

    @Size(max = 20)
    private List<@Size(max = 255) String> responsibilities;

    @Size(max = 20)
    private List<@Size(max = 255) String> achievements;

    @Size(max = 20)
    private List<@Size(max = 255) String> skills;

    private boolean isAcademic;

    @Size(max = 100)
    private String level;

    @Size(max = 100)
    private String institute;

    @Size(max = 100)
    private String location;

    @Size(max = 100)
    private String degree;

    @Size(max = 100)
    private String scoreLabel;

    @Size(max = 100)
    private String scoreValue;
}
