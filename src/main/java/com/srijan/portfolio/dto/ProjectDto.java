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
    @Size(max = 60)
    private String name;

    @Size(max = 60)
    private String type;

    @Size(max = 60)
    private String status;

    @Size(max = 60)
    private String year;

    @NotBlank
    @Size(max = 1000)
    private String overview;

    @Size(max = 12)
    private List<@Size(max = 100) String> techStack;

    @Size(max = 255)
    @Pattern(regexp = "^(https?://.+)?$", message = "Live link must be a valid URL")
    private String liveLink;

    @Size(max = 255)
    @Pattern(regexp = "^(https?://.+)?$", message = "Source link must be a valid URL")
    private String sourceLink;

    @Size(max = 255)
    @Pattern(regexp = "^(https?://.+)?$", message = "PDF link must be a valid URL")
    private String pdfLink;

    @Size(max = 255)
    @Pattern(regexp = "^(https?://.+)?$", message = "Media video must be a valid URL")
    private String mediaVideo;

    @Size(max = 6)
    private List<@Size(max = 255) String> screenshots;

    private boolean isResearch;
}
