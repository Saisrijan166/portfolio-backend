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
public class ContactLinkDto {
    @NotBlank
    @Size(max = 60)
    private String label;

    @NotBlank
    @Pattern(regexp = "^(https?://.+|mailto:.+)$", message = "Link must be a valid URL or mailto link")
    @Size(max = 255)
    private String url;
}
