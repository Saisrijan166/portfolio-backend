package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100)
    private String username;

    @Size(max = 100)
    private String identifier;

    @NotBlank
    private String password;
}
