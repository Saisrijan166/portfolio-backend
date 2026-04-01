package com.srijan.portfolio.service;

import com.srijan.portfolio.config.AiProviderConfig;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.service.ai.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Orchestrator for resume parsing with tri-level fallback:
 * 1. Gemini (multimodal — reads the file directly)
 * 2. Groq (text-only — receives extracted text)
 * 3. Internal parser (regex/heuristic — no AI)
 */
@Service
public class ResumeParseService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParseService.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of("pdf", "docx");
    private static final long DEFAULT_MAX_SIZE = 2 * 1024 * 1024; // 2MB

    private final List<AiProvider> aiProviders;
    private final ResumeTextExtractor textExtractor;
    private final InternalResumeParser internalParser;
    private final AiProviderConfig config;

    public ResumeParseService(
            GeminiAiProvider geminiProvider,
            GroqAiProvider groqProvider,
            ResumeTextExtractor textExtractor,
            InternalResumeParser internalParser,
            AiProviderConfig config) {
        // Order matters: primary first, then fallbacks
        this.aiProviders = List.of(geminiProvider, groqProvider);
        this.textExtractor = textExtractor;
        this.internalParser = internalParser;
        this.config = config;
    }

    /**
     * Parse a resume file with automatic fallback chain.
     */
    public ResumeParseResponseDto parseResume(String fileBase64, String fileType, String fileName) {
        // ─── Validate ────────────────────────────────────────────────────────
        if (fileBase64 == null || fileBase64.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE", "File data is required");
        }

        String normalizedType = normalizeFileType(fileType, fileName);
        if (!SUPPORTED_TYPES.contains(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_TYPE",
                    "Unsupported file type. Only PDF and DOCX are supported.");
        }

        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(fileBase64);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE", "Invalid file data (base64 decode failed)");
        }

        long maxSize = config.getMaxFileSize() > 0 ? config.getMaxFileSize() : DEFAULT_MAX_SIZE;
        if (fileBytes.length > maxSize) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE",
                    "File size exceeds the maximum allowed (" + (maxSize / 1024 / 1024) + " MB)");
        }

        // ─── Extract text (needed for Groq and internal parser) ──────────────
        String extractedText = textExtractor.extractText(fileBytes, normalizedType);

        // ─── Try AI providers in order ───────────────────────────────────────
        for (AiProvider provider : aiProviders) {
            if (!provider.isAvailable()) {
                log.info("Provider {} not available, skipping", provider.getName());
                continue;
            }

            try {
                log.info("Trying provider: {}", provider.getName());
                ResumeParseResponseDto result = provider.parseResume(fileBytes, normalizedType, extractedText);
                if (result != null) {
                    log.info("Successfully parsed resume with provider: {}", provider.getName());
                    return result;
                }
            } catch (Exception e) {
                log.warn("Provider {} failed: {}", provider.getName(), e.getMessage());
            }
        }

        // ─── Final fallback: internal parser ─────────────────────────────────
        log.info("All AI providers failed, using internal parser");
        if (extractedText == null || extractedText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PARSE_FAILED",
                    "Could not extract text from the file. Please try a different file format.");
        }

        return internalParser.parse(extractedText);
    }

    private String normalizeFileType(String fileType, String fileName) {
        if (fileType != null && !fileType.isBlank()) {
            String type = fileType.toLowerCase().trim();
            if (type.contains("pdf"))
                return "pdf";
            if (type.contains("docx") || type.contains("word") || type.contains("openxmlformats"))
                return "docx";
            if (type.contains("doc"))
                return "docx";
            if (type.contains("txt") || type.contains("text"))
                return "txt";
            return type;
        }

        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".pdf"))
                return "pdf";
            if (lower.endsWith(".docx"))
                return "docx";
            if (lower.endsWith(".doc"))
                return "docx";
            if (lower.endsWith(".txt"))
                return "txt";
        }

        return "";
    }
}
