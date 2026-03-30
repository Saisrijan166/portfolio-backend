package com.srijan.portfolio.email;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailTemplateService {

    private static final String TEMPLATE_ROOT = "email/templates/";

    public String render(String templateName, Map<String, String> variables, TenantEmailContext tenantContext) {
        Map<String, String> safeVariables = new HashMap<>();
        if (variables != null) {
            variables.forEach((key, value) -> safeVariables.put(
                    key,
                    isRawToken(key) ? (value == null ? "" : value) : HtmlUtils.htmlEscape(value == null ? "" : value)
            ));
        }

        safeVariables.put("APP_NAME", escape(tenantContext.appName()));
        safeVariables.put("APP_URL", escape(tenantContext.appUrl()));
        
        String tKey = tenantContext.tenantKey();
        if (tKey != null && !tKey.isBlank()) {
            safeVariables.put("DASHBOARD_URL", escape(tenantContext.appUrl() + "/dashboard/" + tKey));
            safeVariables.put("PORTFOLIO_URL", escape(tenantContext.appUrl() + "/" + tKey));
        } else {
            safeVariables.put("DASHBOARD_URL", escape(tenantContext.appUrl() + "/login"));
            safeVariables.put("PORTFOLIO_URL", escape(tenantContext.appUrl()));
        }
        safeVariables.put("SUPPORT_EMAIL", escape(tenantContext.supportEmail()));
        safeVariables.put("PRIMARY_COLOR", escape(tenantContext.primaryColor()));
        safeVariables.put("ACCENT_COLOR", escape(tenantContext.accentColor()));
        safeVariables.put("CURRENT_YEAR", String.valueOf(Year.now().getValue()));
        safeVariables.put("LOGO_SECTION", buildLogoSection(tenantContext));
        safeVariables.put("PREHEADER", escape(variables != null ? variables.getOrDefault("PREHEADER", "") : ""));

        String content = replaceTokens(loadTemplate(templateName + ".html"), safeVariables);
        safeVariables.put("CONTENT", content);
        return replaceTokens(loadTemplate("base-template.html"), safeVariables);
    }

    public String renderDetailRows(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        details.forEach((label, value) -> builder.append("""
                <tr>
                  <td style="padding: 0 0 8px; font-size: 12px; color: #6b7280; letter-spacing: 0.02em;">%s</td>
                  <td style="padding: 0 0 8px; font-size: 13px; color: #111827; text-align: right;">%s</td>
                </tr>
                """.formatted(
                HtmlUtils.htmlEscape(label == null ? "" : label),
                HtmlUtils.htmlEscape(value == null ? "" : value)
        )));
        return builder.toString();
    }

    public String renderParagraphs(String... paragraphs) {
        if (paragraphs == null || paragraphs.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph == null || paragraph.isBlank()) {
                continue;
            }
            builder.append("""
                    <p style="margin: 0 0 14px; font-size: 14px; line-height: 1.7; color: #374151;">%s</p>
                    """.formatted(HtmlUtils.htmlEscape(paragraph)));
        }
        return builder.toString();
    }

    public String renderTextBlock(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return Arrays.stream(value.trim().split("(?:\\r?\\n){2,}"))
                .map(String::trim)
                .filter(paragraph -> !paragraph.isBlank())
                .map(paragraph -> """
                        <p style="margin: 0 0 14px; font-size: 14px; line-height: 1.7; color: #374151;">%s</p>
                        """.formatted(HtmlUtils.htmlEscape(paragraph).replace("\r\n", "\n").replace("\n", "<br />")))
                .reduce("", String::concat);
    }

    private String buildLogoSection(TenantEmailContext tenantContext) {
        if (tenantContext.logoUrl() == null || tenantContext.logoUrl().isBlank()) {
            return "";
        }
        return """
                <div style="margin-bottom: 16px;">
                  <img src="%s" alt="%s logo" style="max-height: 40px; width: auto; display: inline-block;" />
                </div>
                """.formatted(escape(tenantContext.logoUrl()), escape(tenantContext.appName()));
    }

    private String replaceTokens(String template, Map<String, String> variables) {
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered.replaceAll("\\{\\{[A-Z0-9_]+}}", "");
    }

    private String loadTemplate(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_ROOT + fileName);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load email template: " + fileName, exception);
        }
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private boolean isRawToken(String key) {
        return key != null && (key.endsWith("_HTML") || key.endsWith("_ROWS") || key.endsWith("_SECTION"));
    }
}
