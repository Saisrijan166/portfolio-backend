package com.srijan.portfolio.controller;

import com.srijan.portfolio.service.ResumeGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ResumeGeneratorController {

    private final ResumeGeneratorService resumeGeneratorService;

    @GetMapping("/api/public/portfolio/{username}/resume/generate")
    public ResponseEntity<byte[]> generateResume(@PathVariable String username) {
        byte[] pdfBytes = resumeGeneratorService.generateResumePdf(username);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", username + "_resume.pdf");
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/api/public/portfolio/{username}/resume/generate/tex")
    public ResponseEntity<String> generateResumeTex(@PathVariable String username) {
        String texContent = resumeGeneratorService.generateResumeTex(username);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", username + "_resume.tex");
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return ResponseEntity.ok()
                .headers(headers)
                .body(texContent);
    }
}
