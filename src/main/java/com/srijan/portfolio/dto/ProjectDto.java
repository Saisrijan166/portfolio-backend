package com.srijan.portfolio.dto;

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
    private String name;
    private String type;
    private String status;
    private String year;
    private String overview;
    private List<String> techStack;

    private String liveLink;
    private String sourceLink;
    private String pdfLink;

    private String mediaVideo;
    private List<String> screenshots;

    private boolean isResearch;
}
