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
public class CertificationAchievementDto {
    private Long id;

    @NotBlank
    @Pattern(regexp = "^(certification|achievement)$", message = "Type must be certification or achievement")
    private String type;

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 100)
    private String issuer;

    @Size(max = 100)
    private String issuedOn;

    @Size(max = 1000)
    private String description;

    @Size(max = 512)
    @Pattern(regexp = "^(https?://.+)?$", message = "Reference link must be a valid URL")
    private String referenceUrl;

    @Size(max = 512)
    @Pattern(regexp = "^(https?://.+)?$", message = "Image link must be a valid URL")
    private String imageUrl;
}
