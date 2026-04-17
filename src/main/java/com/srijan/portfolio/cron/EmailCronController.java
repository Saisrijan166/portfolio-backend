package com.srijan.portfolio.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/cron")
@RequiredArgsConstructor
public class EmailCronController {

    private final WeeklyEmailService weeklyEmailService;

    @Value("${cron.api.key}")
    private String configuredApiKey;

    @PostMapping("/weekly-email")
    public ResponseEntity<?> triggerWeeklyEmail(@RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        if (apiKey == null || configuredApiKey == null || !MessageDigest.isEqual(apiKey.getBytes(), configuredApiKey.getBytes())) {
            log.warn("Unauthorized invocation of weekly email cron job");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized access to cron endpoint"));
        }

        String jobId = "weekly-email-" + java.time.LocalDate.now().toString() + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Triggered weekly email cron job. Job ID: {}", jobId);
        
        // Asynchronously process sending emails to prevent Render timeout
        weeklyEmailService.sendWeeklyEmailsAsync(jobId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "ACCEPTED",
                "message", "Weekly email job started",
                "jobId", jobId
        ));
    }
}
