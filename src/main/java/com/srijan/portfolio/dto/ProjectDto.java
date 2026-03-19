package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDto {
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String type;

    @NotBlank
    @Size(max = 255)
    private String status;

    @NotBlank
    @Size(max = 255)
    private String year;

    @NotBlank
    @Size(max = 2000)
    private String overview;

    @Size(max = 20)
    private List<String> techStack;

    @Pattern(regexp = "^(https?://.+)?$", message = "Live link must be a valid URL")
    private String liveLink;

    @Pattern(regexp = "^(https?://.+)?$", message = "Source link must be a valid URL")
    private String sourceLink;

    @Pattern(regexp = "^(https?://.+)?$", message = "PDF link must be a valid URL")
    private String pdfLink;

    @Pattern(regexp = "^(https?://.+)?$", message = "Media video must be a valid URL")
    private String mediaVideo;

    @Size(max = 12)
    private List<String> screenshots;

    private boolean isResearch;
}
