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
    @Pattern(
            regexp = "^(https?://(localhost|\\d{1,3}(?:\\.\\d{1,3}){3}|(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,})(?::\\d{1,5})?(?:/\\S*)?|/\\S+)$",
            message = "Resume URL must be a valid absolute URL or absolute path"
    )
    @Size(max = 255)
    private String resumeUrl;

    @Size(max = 150)
    private String lastUpdated;
}
