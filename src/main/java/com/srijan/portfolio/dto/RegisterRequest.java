package com.srijan.portfolio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.srijan.portfolio.validation.PasswordValidation.PASSWORD_MESSAGE;
import static com.srijan.portfolio.validation.PasswordValidation.PASSWORD_PATTERN;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank
    @Size(max = 31)
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,30}$", message = "Username must be 3-31 chars using lowercase letters, numbers, or hyphens")
    private String username;

    @NotBlank
    @Email
    @Size(max = 80)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72)
    @Pattern(
            regexp = PASSWORD_PATTERN,
            message = PASSWORD_MESSAGE
    )
    private String password;
}
