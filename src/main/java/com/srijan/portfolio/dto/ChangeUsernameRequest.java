package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChangeUsernameRequest {
    @NotBlank
    @Size(max = 31)
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,30}$", message = "Username must be 3-31 chars using lowercase letters, numbers, or hyphens")
    private String username;
}
