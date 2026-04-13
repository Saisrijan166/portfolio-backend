package com.srijan.portfolio.service.ai;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Extracts raw text from PDF and DOCX files.
 * Used by text-only AI providers (Groq) and the internal parser.
 */
@Component
public class ResumeTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(ResumeTextExtractor.class);

    public String extractText(byte[] fileBytes, String fileType) {
        if (fileBytes == null || fileBytes.length == 0) {
            return "";
        }

        if (fileType == null || fileType.isBlank()) {
            log.warn("File type is null or blank, cannot extract text");
            return "";
        }

        try {
            return switch (fileType.toLowerCase()) {
                case "pdf" -> extractFromPdf(fileBytes);
                case "docx" -> extractFromDocx(fileBytes);
                case "txt" -> new String(fileBytes, StandardCharsets.UTF_8);
                default -> {
                    log.warn("Unsupported file type for text extraction: {}", fileType);
                    yield "";
                }
            };
        } catch (IOException e) {
            log.error("Failed to extract text from {} file: {}", fileType, e.getMessage());
            return "";
        }
    }

    private String extractFromPdf(byte[] fileBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        }
    }

    private String extractFromDocx(byte[] fileBytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
                XWPFDocument document = new XWPFDocument(bais)) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("\n"));
        }
    }
}
