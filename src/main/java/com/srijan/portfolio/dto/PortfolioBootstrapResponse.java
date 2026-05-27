package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PortfolioBootstrapResponse {
    private String username;
    private PortfolioIdentityDto profile;
    private PortfolioAboutSummaryDto about;
    private DesktopWidgetsDto widgets;
    private AppearanceSettingsDto appearance;
    private String lastUpdated;
    private Long projectCount;
    private Long experienceCount;
    private Double portfolioRating;
}
