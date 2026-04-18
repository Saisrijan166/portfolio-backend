package com.srijan.portfolio.email;

import com.srijan.portfolio.tenant.TenantContext;
import com.srijan.portfolio.util.EnvironmentUtils;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TenantBrandingResolver {

    private static final String DEFAULT_APP_NAME = "PortfolioOS";
    private static final String DEFAULT_PRIMARY_COLOR = "#111827";
    private static final String DEFAULT_ACCENT_COLOR = "#f97316";

    public TenantEmailContext resolveCurrent() {
        return resolve(TenantContext.getTenant());
    }

    public TenantEmailContext resolve(String tenantKey) {
        String sanitizedTenant = sanitizeTenantKey(tenantKey);

        String appName = getTenantOverride(sanitizedTenant, "APP_NAME");
        if (appName == null) {
            appName = firstNonBlank(
                    EnvironmentUtils.get("MAIL_FROM_NAME"),
                    EnvironmentUtils.get("APP_NAME"),
                    toDisplayName(sanitizedTenant),
                    DEFAULT_APP_NAME
            );
        }

        String fromEmail = firstNonBlank(
                EnvironmentUtils.get("MAIL_FROM_EMAIL"),
                EnvironmentUtils.get("MAIL_USERNAME"),
                "no-reply@srijanos.com"
        );

        String appUrl = firstNonBlank(
                getTenantOverride(sanitizedTenant, "APP_URL"),
                EnvironmentUtils.get("APP_URL"),
                EnvironmentUtils.get("AUTH_OAUTH_FRONTEND_SUCCESS_URL"),
                "http://localhost:3000"
        );

        String assetBaseUrl = firstNonBlank(
                EnvironmentUtils.get("NEXT_PUBLIC_ASSET_BASE_URL"),
                appUrl
        );

        return TenantEmailContext.builder()
                .tenantKey(sanitizedTenant)
                .appName(appName)
                .logoUrl(firstNonBlank(
                        assetBaseUrl + "/logos/logo_portfolioos.png"
                ))
                .primaryColor(firstNonBlank(
                        getTenantOverride(sanitizedTenant, "EMAIL_PRIMARY_COLOR"),
                        EnvironmentUtils.get("EMAIL_PRIMARY_COLOR"),
                        DEFAULT_PRIMARY_COLOR
                ))
                .accentColor(firstNonBlank(
                        getTenantOverride(sanitizedTenant, "EMAIL_ACCENT_COLOR"),
                        EnvironmentUtils.get("EMAIL_ACCENT_COLOR"),
                        DEFAULT_ACCENT_COLOR
                ))
                .supportEmail(firstNonBlank(
                        getTenantOverride(sanitizedTenant, "SUPPORT_EMAIL"),
                        EnvironmentUtils.get("SUPPORT_EMAIL"),
                        fromEmail
                ))
                .appUrl(appUrl)
                .assetBaseUrl(assetBaseUrl)
                .fromName(appName)
                .fromEmail(fromEmail)
                .build();
    }

    private String getTenantOverride(String tenantKey, String suffix) {
        if (tenantKey == null || tenantKey.isBlank()) {
            return null;
        }
        return EnvironmentUtils.get("TENANT_" + tenantKey.toUpperCase(Locale.ROOT).replace('-', '_') + "_" + suffix);
    }

    private String sanitizeTenantKey(String tenantKey) {
        return tenantKey == null || tenantKey.isBlank()
                ? null
                : tenantKey.trim().toLowerCase(Locale.ROOT);
    }

    private String toDisplayName(String tenantKey) {
        if (tenantKey == null || tenantKey.isBlank()) {
            return null;
        }
        String normalized = tenantKey.replace('-', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
