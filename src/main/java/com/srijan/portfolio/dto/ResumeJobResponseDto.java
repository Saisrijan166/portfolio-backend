package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeJobResponseDto {
    private Long jobId;
    private String status;
    private ResumeParseResponseDto result;
    private ResumeScoreDto score;
    private String error;
    private boolean cached;
}
