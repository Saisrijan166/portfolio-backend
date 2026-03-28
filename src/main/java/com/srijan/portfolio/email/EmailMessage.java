package com.srijan.portfolio.email;

import lombok.Builder;

import java.util.Map;

@Builder
public record EmailMessage(
        String to,
        String subject,
        String templateName,
        Map<String, String> variables,
        TenantEmailContext tenantContext
) {
}
