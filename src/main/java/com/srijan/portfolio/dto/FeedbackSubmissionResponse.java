package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackSubmissionResponse {
    private Long id;
    private Integer rating;
    private String message;
    private boolean updatedExisting;
    private boolean messageStored;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
