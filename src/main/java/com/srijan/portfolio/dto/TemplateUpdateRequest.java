package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TemplateUpdateRequest {

    @NotBlank(message = "Template value is required")
    @Pattern(
            regexp = "^(linux-os|noir-terminal|brutalist-magazine|spatial-3d-card|editorial-scroll|glassmorphic-zen)$",
            message = "Invalid template selection"
    )
    private String template;
}
