package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SuperAdminStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long verifiedEmailUsers;
    private long unverifiedEmailUsers;
    private long usersCreatedLast7Days;
    private long usersCreatedLast30Days;
}
