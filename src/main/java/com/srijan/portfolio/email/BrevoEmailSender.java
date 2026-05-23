package com.srijan.portfolio.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoEmailSender {

    private static final String BREVO_EMAILS_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate;

    @Value("${brevo.api.key:}")
    private String apiKey;

    public boolean send(String to, String subject, String htmlContent, String fromName, String fromEmail) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Brevo API key is not configured; skipping Brevo delivery to={}", to);
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = Map.of(
                    "sender", Map.of(
                            "name", fromName,
                            "email", fromEmail
                    ),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    BREVO_EMAILS_URL,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Brevo delivery failed to={}", to, e);
            return false;
        }
    }
}
