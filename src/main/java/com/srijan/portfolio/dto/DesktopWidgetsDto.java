package com.srijan.portfolio.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesktopWidgetsDto {
    @Size(max = 8)
    private List<@Size(max = 60) String> bottomLeftPrimary;

    @Size(max = 8)
    private List<@Size(max = 60) String> bottomLeftSecondary;

    @Size(max = 8)
    private List<@Size(max = 60) String> topRight;
}
