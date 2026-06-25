package com.srijan.portfolio.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SuperAdminUpdateUserRequest {

    @Size(max = 20, message = "Status must be under 20 characters")
    private String status;

    @Size(max = 20, message = "Role must be under 20 characters")
    private String role;

    private Boolean emailVerified;
}
