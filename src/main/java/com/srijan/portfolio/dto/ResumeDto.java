package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeDto {
    @NotBlank
    @Pattern(regexp = "^(https?://.+|/.+)$", message = "Resume URL must be an absolute URL or absolute path")
    @Size(max = 512)
    private String resumeUrl;

    @Size(max = 255)
    private String lastUpdated;
}
