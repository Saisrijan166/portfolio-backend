package com.srijan.portfolio.service;

import com.srijan.portfolio.service.ai.SummarizeGroqClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class SectionSummarizeService {

    private static final Logger log = LoggerFactory.getLogger(SectionSummarizeService.class);
    private static final String CACHE_TYPE = "section_summary_v1";
    private static final int MAX_INPUT_CHARS = 12000;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final String SYSTEM_PROMPT = """
            You are a portfolio assistant. Generate a highly concise, impressive summary of the provided section.
            Return the insight in structured, readable format using a few bullet points if necessary.
            Focus entirely on the most important technical achievements, impact, and skills.
            Do not use aggressive markdown, keep it clean. Do not wrap in markdown blocks.
            """;

    private final ResumeAiCacheService resumeAiCacheService;
    private final SummarizeGroqClient summarizeGroqClient;

    public String summarizeSection(String sectionType, String rawContent, boolean forceRefresh) {
        try {
            String normalizedSectionType = normalizeSectionType(sectionType);
            String sanitizedContent = sanitizeContent(rawContent);
            String cacheHash = resumeAiCacheService.sha256(normalizedSectionType + "::" + sanitizedContent);

            if (!forceRefresh) {
                return resumeAiCacheService.read(CACHE_TYPE, cacheHash, String.class)
                        .filter(summary -> !summary.isBlank())
                        .orElseGet(() -> generateAndCacheSummary(normalizedSectionType, sanitizedContent, cacheHash));
            }
            
            return generateAndCacheSummary(normalizedSectionType, sanitizedContent, cacheHash);
        } catch (Exception e) {
            String msg = e.getMessage() + " ||| " + e.getClass().getName();
            if (e.getCause() != null) {
                msg += " ||| Caused by: " + e.getCause().getMessage() + " " + e.getCause().getClass().getName();
            }
            throw new RuntimeException(msg, e);
        }
    }

    private String generateAndCacheSummary(String sectionType, String sanitizedContent, String cacheHash) {
        String summary = summarizeGroqClient.generateSummary(
                SYSTEM_PROMPT,
                buildUserPrompt(sectionType, sanitizedContent)
        );
        String cleanedSummary = sanitizeSummary(summary);
        resumeAiCacheService.write(CACHE_TYPE, cacheHash, cleanedSummary);
        return cleanedSummary;
    }

    private String buildUserPrompt(String sectionType, String content) {
        return """
                Summarize the following %s portfolio section in a concise and engaging way.
                Keep it simple and impactful.

                %s
                """.formatted(sectionType, content);
    }

    private String normalizeSectionType(String sectionType) {
        String cleaned = collapseWhitespace(sectionType).toLowerCase(Locale.ROOT);
        return cleaned.isBlank() ? "general" : cleaned;
    }

    private String sanitizeContent(String rawContent) {
        String withoutHtml = HTML_TAG_PATTERN.matcher(rawContent == null ? "" : rawContent).replaceAll(" ");
        String collapsed = collapseWhitespace(withoutHtml);
        if (collapsed.length() <= MAX_INPUT_CHARS) {
            return collapsed;
        }
        return collapsed.substring(0, MAX_INPUT_CHARS);
    }

    private String sanitizeSummary(String value) {
        return collapseWhitespace(HTML_TAG_PATTERN.matcher(value == null ? "" : value).replaceAll(" "));
    }

    private String collapseWhitespace(String value) {
        return WHITESPACE_PATTERN.matcher(value == null ? "" : value).replaceAll(" ").trim();
    }
}
