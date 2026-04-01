package com.srijan.portfolio.service.ai;

import com.srijan.portfolio.dto.ResumeParseResponseDto;

/**
 * Provider-agnostic interface for AI-based resume parsing.
 * Implementations can wrap any AI provider (Gemini, Groq, etc.)
 */
public interface AiProvider {

    /** Human-readable provider name (e.g. "gemini", "groq") */
    String getName();

    /** Whether this provider is configured and can accept requests */
    boolean isAvailable();

    /**
     * Parse a resume file and return structured data.
     * 
     * @param fileBytes     raw file bytes
     * @param fileType      "pdf" or "docx"
     * @param extractedText pre-extracted text (used by text-only providers)
     * @return structured resume data
     */
    ResumeParseResponseDto parseResume(byte[] fileBytes, String fileType, String extractedText) throws Exception;
}
