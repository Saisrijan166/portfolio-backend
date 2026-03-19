package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,30}$", message = "Username format is invalid")
    private String username;

    @NotBlank
    private String password;
}
