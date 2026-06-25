package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SuperAdminUserPageDto {
    private List<SuperAdminUserDto> users;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private SuperAdminStatsDto stats;
}
