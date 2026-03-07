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
public class ExperienceDto {
    private Long id;
    private String company;
    private String roleTitle;
    private String duration;
    private Integer startYear;
    private Integer endYear;
    private boolean isCurrent;

    private List<String> responsibilities;
    private List<String> achievements;
    private List<String> skills;

    private boolean isAcademic;

    private String level;
    private String institute;
    private String location;
    private String degree;
    private String scoreLabel;
    private String scoreValue;
}
