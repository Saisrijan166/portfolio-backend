package com.srijan.portfolio.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackSubmitRequest {

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 600, message = "Message must be 600 characters or fewer")
    private String message;

    @Size(max = 60, message = "Name must be 60 characters or fewer")
    private String name;

    @AssertTrue(message = "Provide at least a rating or a message")
    public boolean isPayloadPresent() {
        return rating != null || (message != null && !message.trim().isEmpty());
    }
}
