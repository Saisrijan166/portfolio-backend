package com.srijan.portfolio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkillDto {
    private Long id;

    @Size(max = 30)
    private String domain;

    @NotBlank
    @Size(max = 30)
    private String name;

    @Min(0)
    @Max(4)
    private int level;

    private boolean isMetaSkill;

    @Size(max = 100)
    private String metaDescription;
}
