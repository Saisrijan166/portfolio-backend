package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class SuperAdminBulkActionRequest {

    @NotEmpty(message = "At least one user ID is required")
    @Size(max = 200, message = "Cannot perform bulk action on more than 200 users at once")
    private List<Long> userIds;

    @NotNull(message = "Action is required")
    private BulkAction action;

    public enum BulkAction {
        SUSPEND,
        ACTIVATE,
        VERIFY_EMAIL
    }
}
