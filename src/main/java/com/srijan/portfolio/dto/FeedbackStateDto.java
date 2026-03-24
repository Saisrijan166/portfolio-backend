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
public class FeedbackStateDto {
    private Integer rating;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
