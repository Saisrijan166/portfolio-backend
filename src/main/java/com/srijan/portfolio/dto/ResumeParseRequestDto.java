package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResumeParseRequestDto {

    @NotBlank(message = "File data is required")
    private String fileBase64;

    private String fileType; // "pdf", "docx" — validated in service layer

    @NotBlank(message = "File name is required")
    private String fileName;
}
