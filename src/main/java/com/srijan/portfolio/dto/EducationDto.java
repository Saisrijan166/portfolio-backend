package com.srijan.portfolio.dto;

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
public class EducationDto {
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String level;

    @NotBlank
    @Size(max = 255)
    private String institute;

    @NotBlank
    @Size(max = 255)
    private String location;

    @NotBlank
    @Size(max = 255)
    private String degree;

    @Size(max = 255)
    private String scoreLabel;

    @Size(max = 255)
    private String scoreValue;

    @NotBlank
    @Size(max = 255)
    private String duration;

    private boolean deleted;
}
