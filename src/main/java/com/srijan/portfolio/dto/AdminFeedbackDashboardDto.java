package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFeedbackDashboardDto {
    private ReceivedFeedbackPageDto received;
    private FeedbackStateDto platform;
}
