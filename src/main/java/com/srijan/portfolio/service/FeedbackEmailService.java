package com.srijan.portfolio.service;

import com.srijan.portfolio.email.EmailService;
import com.srijan.portfolio.entity.PlatformFeedback;
import com.srijan.portfolio.entity.PortfolioFeedback;
import com.srijan.portfolio.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackEmailService {

    private final EmailService emailService;

    @Value("${feedback.mail.from:no-reply@portfolio.local}")
    private String fromAddress;

    @Value("${feedback.mail.developer-email:dev@yourplatform.com}")
    private String developerEmail;

    public void sendPortfolioFeedbackNotification(User owner, PortfolioFeedback feedback) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Portfolio", "@" + owner.getUsername());
        details.put("Submitted by", safeSubmitterName(feedback.getSubmitterName()));
        details.put("Rating", safeRating(feedback.getRating()));
        details.put("Message", safe(feedback.getMessage()));
        details.put("Submitted at", String.valueOf(feedback.getUpdatedAt()));

        send(
                owner.getEmail(),
                "New portfolio feedback for @" + owner.getUsername(),
                "New portfolio feedback received",
                "A new public feedback submission was received for your portfolio.",
                details
        );
    }

    public void sendPlatformFeedbackNotification(String subjectContext, PlatformFeedback feedback) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Context", subjectContext);
        details.put("Portfolio owner", feedback.getPortfolioOwner() != null ? "@" + feedback.getPortfolioOwner().getUsername() : "n/a");
        details.put("Submitted by", buildSubmitterContext(feedback));
        details.put("Rating", safeRating(feedback.getRating()));
        details.put("Message", safe(feedback.getMessage()));
        details.put("Updated at", String.valueOf(feedback.getUpdatedAt()));

        send(
                developerEmail,
                "Platform feedback: " + subjectContext,
                "Platform feedback received",
                "A new platform feedback message was submitted from the application.",
                details
        );
    }

    private void send(String to, String subject, String title, String intro, Map<String, String> details) {
        if (to == null || to.isBlank()) {
            log.info("Mail delivery skipped subject={} to={}", subject, to);
            return;
        }

        try {
            emailService.sendNotificationEmail(
                    to,
                    subject,
                    title,
                    intro,
                    details,
                    "This notification was generated automatically from " + fromAddress + ".",
                    null
            );
        } catch (Exception exception) {
            log.warn("Failed to send feedback email subject={} to={}", subject, to, exception);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(no message)" : value;
    }

    private String safeRating(Integer value) {
        return value == null ? "No star rating" : value + "/5";
    }

    private String safeSubmitterName(String value) {
        return value == null || value.isBlank() ? "Anonymous" : value;
    }

    private String buildSubmitterContext(PlatformFeedback feedback) {
        String customName = feedback.getSubmitterName();
        if (customName != null && !customName.isBlank() && feedback.getSubmittedByUser() != null) {
            return "%s (@%s)".formatted(customName, feedback.getSubmittedByUser().getUsername());
        }
        if (customName != null && !customName.isBlank()) {
            return customName;
        }
        if (feedback.getSubmittedByUser() != null) {
            return "@" + feedback.getSubmittedByUser().getUsername();
        }
        return "Anonymous";
    }
}
