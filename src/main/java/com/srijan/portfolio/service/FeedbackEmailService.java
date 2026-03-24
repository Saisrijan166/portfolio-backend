package com.srijan.portfolio.service;

import com.srijan.portfolio.entity.PlatformFeedback;
import com.srijan.portfolio.entity.PortfolioFeedback;
import com.srijan.portfolio.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${feedback.mail.from:no-reply@portfolio.local}")
    private String fromAddress;

    @Value("${feedback.mail.developer-email:dev@yourplatform.com}")
    private String developerEmail;

    public void sendPortfolioFeedbackNotification(User owner, PortfolioFeedback feedback) {
        String subject = "New portfolio feedback for @" + owner.getUsername();
        String body = """
                New public feedback arrived for @%s.

                Submitted by: %s
                Rating: %s
                Message: %s
                Submitted at: %s
                """.formatted(
                owner.getUsername(),
                safeSubmitterName(feedback.getSubmitterName()),
                safeRating(feedback.getRating()),
                safe(feedback.getMessage()),
                feedback.getUpdatedAt()
        );

        send(owner.getEmail(), subject, body);
    }

    public void sendPlatformFeedbackNotification(String subjectContext, PlatformFeedback feedback) {
        String ownerContext = feedback.getPortfolioOwner() != null
                ? "Portfolio owner: @" + feedback.getPortfolioOwner().getUsername()
                : "Portfolio owner: n/a";
        String submitterContext = buildSubmitterContext(feedback);

        String body = """
                Platform feedback received.

                Context: %s
                %s
                %s
                Rating: %s
                Message: %s
                Updated at: %s
                """.formatted(
                subjectContext,
                ownerContext,
                submitterContext,
                safeRating(feedback.getRating()),
                safe(feedback.getMessage()),
                feedback.getUpdatedAt()
        );

        send(developerEmail, "Platform feedback: " + subjectContext, body);
    }

    private void send(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || to == null || to.isBlank()) {
            log.info("Mail delivery skipped subject={} to={} body={}", subject, to, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
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
            return "Submitted by: %s (@%s)".formatted(customName, feedback.getSubmittedByUser().getUsername());
        }
        if (customName != null && !customName.isBlank()) {
            return "Submitted by: " + customName;
        }
        if (feedback.getSubmittedByUser() != null) {
            return "Submitted by: @" + feedback.getSubmittedByUser().getUsername();
        }
        return "Submitted by: Anonymous";
    }
}
