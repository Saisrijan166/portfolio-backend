package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SectionSummarizeRequestDto(
        @NotBlank(message = "Section type is required")
        @Size(max = 60, message = "Section type must be 60 characters or fewer")
        String sectionType,
        @NotBlank(message = "Section content is required")
        @Size(max = 20000, message = "Section content must be 20000 characters or fewer")
        String content,
        Boolean forceRefresh
) {
}
