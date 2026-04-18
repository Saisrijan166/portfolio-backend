package com.srijan.portfolio.email;

import lombok.Builder;

@Builder
public record TenantEmailContext(
        String tenantKey,
        String appName,
        String logoUrl,
        String primaryColor,
        String accentColor,
        String supportEmail,
        String appUrl,
        String assetBaseUrl,
        String fromName,
        String fromEmail
) {
}
