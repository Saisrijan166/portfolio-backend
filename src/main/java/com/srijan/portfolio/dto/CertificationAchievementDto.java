package com.srijan.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.srijan.portfolio.config.FlexibleLocalDateDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    @Size(max = 50)
    private String title;

    @Size(max = 50)
    private String issuer;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate issuedOn;

    @Size(max = 200)
    private String description;

    @Size(max = 512)
    @Pattern(regexp = "^(https?://.+)?$", message = "Reference link must be a valid URL")
    private String referenceUrl;

    @Size(max = 512)
    @Pattern(regexp = "^(https?://.+)?$", message = "Image link must be a valid URL")
    private String imageUrl;
}
