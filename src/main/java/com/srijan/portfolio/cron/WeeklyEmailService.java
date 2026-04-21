package com.srijan.portfolio.cron;

import com.srijan.portfolio.email.EmailService;
import com.srijan.portfolio.email.TenantBrandingResolver;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyEmailService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final TenantBrandingResolver tenantBrandingResolver;

    @Value("${email.batch.size:50}")
    private int batchSize;

    @Value("${email.batch.delay:2000}")
    private long batchDelayMs;

    // We implement 5 variants.
    private static final List<EmailVariant> VARIANTS = List.of(
            new EmailVariant(
                    "💡 Quick check… when was your last portfolio update?",
                    "\nIt’s been a little quiet on your portfolio lately.\nNo updates. No changes. Just sitting there.\nMeanwhile, new ideas, projects, and skills are probably piling up on your side.\nMaybe it’s time to bring it back to life.",
                    "Open your portfolio"
            ),
            new EmailVariant(
                    "It’s waiting for you 👀",
                    "\nYour portfolio misses you.\nIt’s been waiting… probably wondering when you’ll come back and add something new.\nEven a small update makes a difference.",
                    "Give it a quick update"
            ),
            new EmailVariant(
                    "This is not what you want 😄",
                    "\nIf someone opens your portfolio today…\nThey’ll see exactly what they saw last week.\nNo new projects. No updates.\nThat might not be the impression you want to leave.",
                    "Make a quick update"
            ),
            new EmailVariant(
                    "🧠 One small update can change everything",
                    "\nMost people don’t realize this—\nOne small update to a portfolio can completely change how it’s perceived.\nA new project. A better description. A small tweak.\nYou don’t need a full overhaul.\nJust one step.",
                    "Update something today"
            ),
            new EmailVariant(
                    "⏳ Time moved forward. Did you?",
                    "\nAnother week passed.\nYour skills probably improved.\nYour ideas probably evolved.\nBut your portfolio?\nStill the same.",
                    "Sync it with your current self"
            )
    );

    @Async
    public void sendWeeklyEmailsAsync(String jobId) {
        log.info("Starting background processing for Job ID: {}", jobId);

        // Calculate variant based on week of year (Deterministic Locale)
        int weekOfYear = LocalDate.now().get(WeekFields.of(Locale.US).weekOfWeekBasedYear());
        EmailVariant selectedVariant = VARIANTS.get(weekOfYear % VARIANTS.size());

        int pageNumber = 0;
        long totalProcessed = 0;

        while (true) {
            Pageable pageable = PageRequest.of(pageNumber, batchSize);
            Page<User> userPage = userRepository.findByEmailIsNotNull(pageable);

            List<User> users = userPage.getContent();
            if (users.isEmpty()) {
                break;
            }

            for (User user : users) {
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    continue; // Double check
                }

                String maskedEmail = maskEmail(user.getEmail());

                try {
                    Map<String, String> variables = new HashMap<>();
                    variables.put("SUBJECT", selectedVariant.subject());
                    String escapedUsername = user.getUsername() != null ? " " + HtmlUtils.htmlEscape(user.getUsername()) : "";
                    variables.put("GREETING", "Hey" + escapedUsername + ",");
                    // Security: Standardize newline to br and escape text content before adding HTML
                    String sanitizedBody = HtmlUtils.htmlEscape(selectedVariant.body()).replace("\n", "<br/>");
                    variables.put("MESSAGE_HTML", sanitizedBody);
                    variables.put("CTA_TEXT", selectedVariant.ctaText());
                    variables.put("PREHEADER", "Your portfolio pulse.");

                    emailService.sendTemplatedEmail(
                            user.getEmail(),
                            selectedVariant.subject(),
                            "portfolio-pulse-email",
                            variables,
                            tenantBrandingResolver.resolveCurrent()
                    );
                    log.debug("Sent weekly email to: {}", maskedEmail);
                } catch (Exception e) {
                    log.error("Failed to send weekly email to: {}", maskedEmail, e);
                }
            }

            totalProcessed += users.size();
            log.info("Processed batch {} ({} users) for Job ID: {}", pageNumber, users.size(), jobId);

            if (!userPage.hasNext()) {
                 break;
            }

            pageNumber++;
            
            // Sleep to avoid rate limiting
            try {
                Thread.sleep(batchDelayMs);
            } catch (InterruptedException e) {
                log.warn("Batch sleeping interrupted", e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("Completed weekly email cron job: {}. Total users processed: {}", jobId, totalProcessed);
    }
    

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "masked-email";
        return email.replaceAll("(^.{2}).*(?=@)", "$1****");
    }

    private record EmailVariant(String subject, String body, String ctaText) { }
}
