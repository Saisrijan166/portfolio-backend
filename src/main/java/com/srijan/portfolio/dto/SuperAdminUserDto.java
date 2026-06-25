package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SuperAdminUserDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
    private boolean emailVerified;
    private boolean hasPassword;
    private String activeTemplate;
    private List<String> authProviders;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
