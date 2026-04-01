package com.srijan.portfolio.dto;

import lombok.Data;

@Data
public class ResumeParseRequestDto {
    private String fileBase64;
    private String fileType; // "pdf", "docx"
    private String fileName;
}
