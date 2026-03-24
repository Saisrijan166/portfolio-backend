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
public class ReceivedFeedbackDto {
    private Long id;
    private String submitterName;
    private Integer rating;
    private String message;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
