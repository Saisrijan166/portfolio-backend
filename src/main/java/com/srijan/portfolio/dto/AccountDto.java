package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {
    private Long userId;
    private String username;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}
