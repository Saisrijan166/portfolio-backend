package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.SectionSummarizeRequestDto;
import com.srijan.portfolio.service.SectionSummarizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Validated
@RequiredArgsConstructor
public class SectionSummarizeController {

    private static final Pattern CHUNK_PATTERN = Pattern.compile("\\S+\\s*");
    private static final long CHUNK_DELAY_MS = 28L;

    private final SectionSummarizeService sectionSummarizeService;

    @Transactional
    @PostMapping(value = "/api/ai/summarize-section", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> summarizeSection(@Valid @RequestBody SectionSummarizeRequestDto request) {
        boolean force = request.forceRefresh() != null && request.forceRefresh();
        String summary = sectionSummarizeService.summarizeSection(request.sectionType(), request.content(), force);
        final String finalSummary = summary;

        StreamingResponseBody stream = outputStream -> {
            Matcher matcher = CHUNK_PATTERN.matcher(finalSummary);
            try {
                while (matcher.find()) {
                    outputStream.write(matcher.group().getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    sleepQuietly();
                }
            } catch (Exception ex) {
                // Ignore broken pipe or client aborts
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(stream);
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(CHUNK_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
