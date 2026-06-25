package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class SuperAdminBulkEmailRequest {

    @NotEmpty(message = "At least one user ID is required")
    @Size(max = 500, message = "Cannot send to more than 500 users at once")
    private List<Long> userIds;

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must be under 200 characters")
    private String subject;

    @NotBlank(message = "Message body is required")
    @Size(max = 5000, message = "Message body must be under 5000 characters")
    private String body;
}
