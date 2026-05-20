package com.srijan.portfolio.email;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;

@Slf4j
@Service
@RequiredArgsConstructor
@Async
public class EmailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z").withZone(ZoneId.systemDefault());

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailTemplateService emailTemplateService;
    private final TenantBrandingResolver tenantBrandingResolver;

    public void sendTemplatedEmail(
            String to,
            String subject,
            String templateName,
            Map<String, String> variables,
            TenantEmailContext tenantContext
    ) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("Mail delivery is not configured");
        }

        TenantEmailContext resolvedTenant = tenantContext != null ? tenantContext : tenantBrandingResolver.resolveCurrent();
        String html = emailTemplateService.render(templateName, variables, resolvedTenant);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mimeMessage.setFrom(new InternetAddress(
                    resolvedTenant.fromEmail(),
                    resolvedTenant.fromName(),
                    StandardCharsets.UTF_8.name()
            ));
            mailSender.send(mimeMessage);
        } catch (Exception exception) {
            log.warn("Failed to send templated email template={} to={}", templateName, to, exception);
            throw new IllegalStateException("Failed to send email", exception);
        }
    }

    public void sendOtpEmail(String to, String otp, int expiryMinutes, String title, String subject, String message, TenantEmailContext tenantContext) {
        sendTemplatedEmail(
                to,
                subject,
                "otp-email",
                Map.of(
                        "PREHEADER", title + " from " + safe(tenantContext != null ? tenantContext.appName() : tenantBrandingResolver.resolveCurrent().appName()),
                        "TITLE", title,
                        "MESSAGE", message,
                        "OTP", safe(otp.trim()),
                        "EXPIRY", expiryMinutes + " minutes"
                ),
                tenantContext
        );
    }

    public void sendNotificationEmail(
            String to,
            String subject,
            String title,
            String intro,
            Map<String, String> details,
            String securityNote,
            TenantEmailContext tenantContext
    ) {
        sendTemplatedEmail(
                to,
                subject,
                "notification-email",
                Map.of(
                        "PREHEADER", title,
                        "TITLE", safe(title),
                        "INTRO_HTML", emailTemplateService.renderParagraphs(intro),
                        "DETAILS_ROWS", emailTemplateService.renderDetailRows(details),
                        "SECURITY_NOTE", safe(securityNote)
                ),
                tenantContext
        );
    }

    public void sendContactMessageEmail(
            String to,
            String portfolioUsername,
            String subjectLine,
            String message,
            TenantEmailContext tenantContext
    ) {
        sendTemplatedEmail(
                to,
                "New contact message: " + safe(subjectLine),
                "contact-message-email",
                Map.of(
                        "PREHEADER", "New contact message for @" + safe(portfolioUsername),
                        "TITLE", "New contact message received",
                        "INTRO_HTML", emailTemplateService.renderParagraphs(
                                "A visitor sent you a message through your public Portfoliooss contact app."
                        ),
                        "CONTACT_SUBJECT", safe(subjectLine),
                        "CONTACT_MESSAGE_HTML", emailTemplateService.renderTextBlock(message),
                        "PORTFOLIO_USERNAME", safe(portfolioUsername)
                ),
                tenantContext
        );
    }

    public void sendLoginAlert(String to, String loginMethod, String ipAddress, String deviceInfo, TenantEmailContext tenantContext) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Sign-in method", safe(loginMethod));
        details.put("IP address", safe(ipAddress));
        details.put("Device", safe(deviceInfo));
        details.put("Time", DATE_TIME_FORMATTER.format(java.time.Instant.now()));
        sendNotificationEmail(
                to,
                "New sign-in to your account",
                "New sign-in detected",
                "We noticed a successful sign-in to your account. If this was you, no action is needed.",
                details,
                "If this sign-in was not you, change your password immediately and review your account activity.",
                tenantContext
        );
    }

    public void sendThanksgivingEmail(String to, String username, LocalDateTime createdAt, TenantEmailContext tenantContext) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Username", safe(username));
        details.put("Email address", safe(to));
        LocalDateTime actualCreatedAt = createdAt != null ? createdAt : LocalDateTime.now();
        details.put("Created at", DATE_TIME_FORMATTER.format(actualCreatedAt.atZone(ZoneId.systemDefault())));
        sendTemplatedEmail(
                to,
                "Thank you for creating your account 🎉",
                "thanksgiving-email",
                Map.of(
                        "PREHEADER", "Welcome to your new account",
                        "TITLE", "Thank you for joining us ✨",
                        "USERNAME", safe(username),
                        "MESSAGE", "Your account has been successfully created. We are thrilled to welcome you to our community! Your space is now ready for you.",
                        "DETAILS_ROWS", emailTemplateService.renderDetailRows(details),
                        "FOOTER_NOTE", "You can now customize your portfolio and access your developer dashboard using the links below."
                ),
                tenantContext
        );
    }

    public void sendPasswordChangedEmail(String to, boolean fromResetFlow, TenantEmailContext tenantContext) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Updated at", DATE_TIME_FORMATTER.format(java.time.Instant.now()));
        details.put("Change type", fromResetFlow ? "Password reset" : "Password changed");
        sendNotificationEmail(
                to,
                fromResetFlow ? "Password has been reset" : "Password has been changed",
                fromResetFlow ? "Password reset successful" : "Password changed successfully",
                fromResetFlow
                        ? "Your account password was reset successfully. You can now sign in with your new password."
                        : "Your account password was updated successfully.",
                details,
                "If you did not make this change, secure your account right away.",
                tenantContext
        );
    }

    public void sendUsernameChangedEmail(String to, String oldUsername, String newUsername, TenantEmailContext tenantContext) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Previous username", safe(oldUsername));
        details.put("New username", safe(newUsername));
        details.put("Updated at", DATE_TIME_FORMATTER.format(java.time.Instant.now()));
        sendNotificationEmail(
                to,
                "Username has been updated",
                "Username updated",
                "Your sign-in username and public portfolio URL were updated successfully.",
                details,
                "Use your new username the next time you sign in.",
                tenantContext
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Unavailable" : value;
    }
}
