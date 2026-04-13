package com.srijan.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.config.AiProviderConfig;
import com.srijan.portfolio.dto.ResumeRegenerateRequestDto;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import com.srijan.portfolio.dto.ResumeScoreDto;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.service.ai.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Production-grade resume parsing service with:
 * - secure file validation
 * - AI orchestration with fallback + retry
 * - validation and cache layer
 * - internal parser fallback
 */
@Service
public class ResumeParseService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParseService.class);

    private static final long DEFAULT_MAX_SIZE = 2 * 1024 * 1024; // 2MB

    private final ResumeTextExtractor textExtractor;
    private final InternalResumeParser internalParser;
    private final AiProviderConfig config;
    private final AiOrchestratorService aiOrchestratorService;
    private final ResumeValidator resumeValidator;
    private final ResumePromptFactory resumePromptFactory;
    private final ResumeAiCacheService resumeAiCacheService;
    private final ResumeScoringService resumeScoringService;
    private final ObjectMapper objectMapper;

    public ResumeParseService(
            ResumeTextExtractor textExtractor,
            InternalResumeParser internalParser,
            AiProviderConfig config,
            AiOrchestratorService aiOrchestratorService,
            ResumeValidator resumeValidator,
            ResumePromptFactory resumePromptFactory,
            ResumeAiCacheService resumeAiCacheService,
            ResumeScoringService resumeScoringService,
            ObjectMapper objectMapper) {
        this.textExtractor = textExtractor;
        this.internalParser = internalParser;
        this.config = config;
        this.aiOrchestratorService = aiOrchestratorService;
        this.resumeValidator = resumeValidator;
        this.resumePromptFactory = resumePromptFactory;
        this.resumeAiCacheService = resumeAiCacheService;
        this.resumeScoringService = resumeScoringService;
        this.objectMapper = objectMapper;
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
        Set<String> supportedTypes = Set.copyOf(config.getAllowedTypes());
        if (!supportedTypes.contains(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_TYPE",
                    "Unsupported file type. Supported types: " + supportedTypes.stream().collect(Collectors.joining(", ")));
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

        validateMagicBytes(fileBytes, normalizedType);

        // ─── Extract text (needed for Groq and internal parser) ──────────────
        String extractedText = textExtractor.extractText(fileBytes, normalizedType);
        String resumeHash = resumeAiCacheService.sha256(extractedText);

        ResumeParseResponseDto cached = resumeAiCacheService.read("resume-parse", resumeHash, ResumeParseResponseDto.class)
                .map(resumeValidator::clean)
                .orElse(null);
        if (cached != null) {
            log.info("resume.parse.cache_hit hash={}", resumeHash);
            ResumeParseResponseDto cachedCopy = resumeValidator.clean(cached);
            cachedCopy.setProvider(firstNonBlank(cachedCopy.getProvider(), "cache"));
            return cachedCopy;
        }

        // ─── Try AI providers in priority order via orchestrator ────────────
        try {
            ResumeParseResponseDto aiResult = aiOrchestratorService.parseResume(
                    fileBytes,
                    normalizedType,
                    extractedText,
                    resumeValidator::isValid
            );
            ResumeParseResponseDto validated = resumeValidator.clean(aiResult);
            resumeAiCacheService.write("resume-parse", resumeHash, validated);
            return validated;
        } catch (ApiException exception) {
            log.warn("resume.parse.ai_failed reason={}", exception.getMessage());
        }

        // ─── Final fallback: internal parser ─────────────────────────────────
        log.info("All AI providers failed, using internal parser");
        if (extractedText == null || extractedText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PARSE_FAILED",
                    "Could not extract text from the file. Please try a different file format.");
        }

        ResumeParseResponseDto fallback = resumeValidator.clean(internalParser.parse(extractedText));
        resumeAiCacheService.write("resume-parse", resumeHash, fallback);
        return fallback;
    }

    public ResumeParseResponseDto regenerateSections(ResumeRegenerateRequestDto request) {
        if (request == null || request.getExistingResume() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REGENERATION_REQUEST", "Existing resume data is required");
        }
        if (request.getSections() == null || request.getSections().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REGENERATION_REQUEST", "At least one section is required");
        }
        if (request.getSections().size() > config.getMaxRegeneratedSections()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "TOO_MANY_SECTION_REQUESTS",
                    "Too many sections requested for regeneration"
            );
        }

        String cacheHash = resumeAiCacheService.hashObject(request);
        ResumeParseResponseDto cached = resumeAiCacheService.read("resume-regenerate", cacheHash, ResumeParseResponseDto.class)
                .orElse(null);
        if (cached != null) {
            return resumeValidator.clean(cached);
        }

        String rawJson = aiOrchestratorService.generateJson(
                "You improve resume sections while preserving facts.",
                resumePromptFactory.buildSectionRegenerationPrompt(request.getSections(), request.getExistingResume()),
                this::isJsonObject
        );

        try {
            ResumeParseResponseDto baseCopy = objectMapper.readValue(
                    objectMapper.writeValueAsBytes(request.getExistingResume()),
                    ResumeParseResponseDto.class
            );
            ResumeParseResponseDto merged = objectMapper
                    .readerForUpdating(baseCopy)
                    .readValue(rawJson, ResumeParseResponseDto.class);
            ResumeParseResponseDto cleaned = resumeValidator.clean(merged);
            resumeAiCacheService.write("resume-regenerate", cacheHash, cleaned);
            return cleaned;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "RESUME_REGENERATION_FAILED", "Failed to regenerate selected sections");
        }
    }

    public ResumeScoreDto scoreResume(ResumeParseResponseDto resume) {
        return resumeScoringService.score(resumeValidator.clean(resume));
    }

    private String normalizeFileType(String fileType, String fileName) {
        if (fileType != null && !fileType.isBlank()) {
            String type = fileType.toLowerCase().trim();
            if (type.contains("pdf"))
                return "pdf";
            if (type.contains("docx") || type.contains("word") || type.contains("openxmlformats"))
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
            if (lower.endsWith(".txt"))
                return "txt";
        }

        return "";
    }

    private void validateMagicBytes(byte[] fileBytes, String normalizedType) {
        if ("pdf".equals(normalizedType)) {
            if (fileBytes.length < 4 || fileBytes[0] != 0x25 || fileBytes[1] != 0x50 || fileBytes[2] != 0x44 || fileBytes[3] != 0x46) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PDF", "Uploaded file does not appear to be a valid PDF");
            }
        }
        if ("docx".equals(normalizedType)) {
            if (fileBytes.length < 4 || fileBytes[0] != 0x50 || fileBytes[1] != 0x4B) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DOCX", "Uploaded file does not appear to be a valid DOCX");
            }
        }
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private boolean isJsonObject(String value) {
        return value != null && value.trim().startsWith("{") && value.trim().endsWith("}");
    }
}
