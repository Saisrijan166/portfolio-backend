package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedFeedbackPageDto {
    private List<ReceivedFeedbackDto> items;
    private int page;
    private int size;
    private int totalPages;
    private long totalItems;
    private long feedbackCount;
    private double averageRating;
}
