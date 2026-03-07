package com.srijan.portfolio.dto;

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
    private String level;
    private String institute;
    private String location;
    private String degree;
    private String scoreLabel;
    private String scoreValue;
    private String duration;
    private boolean deleted;
}
