package com.srijan.portfolio.service.ai;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;

/**
 * Extracts raw text from PDF and DOCX files.
 * Used by text-only AI providers (Groq) and the internal parser.
 */
@Component
public class ResumeTextExtractor {

    public String extractText(byte[] fileBytes, String fileType) {
        if (fileBytes == null || fileBytes.length == 0) {
            return "";
        }

        try {
            return switch (fileType.toLowerCase()) {
                case "pdf" -> extractFromPdf(fileBytes);
                case "docx" -> extractFromDocx(fileBytes);
                case "txt" -> new String(fileBytes);
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private String extractFromPdf(byte[] fileBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        }
    }

    private String extractFromDocx(byte[] fileBytes) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
                XWPFDocument document = new XWPFDocument(bais)) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("\n"));
        }
    }
}
