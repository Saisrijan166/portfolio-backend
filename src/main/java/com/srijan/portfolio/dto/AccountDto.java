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
public class AccountDto {
    private Long userId;
    private String username;
    private String email;
    private String role;
    private boolean emailVerified;
    private String status;
    private boolean hasPassword;
    private List<String> providers;
    private LocalDateTime createdAt;
}
