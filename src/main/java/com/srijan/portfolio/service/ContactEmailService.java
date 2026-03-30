package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.ContactMessageRequest;
import com.srijan.portfolio.email.EmailService;
import com.srijan.portfolio.email.TenantBrandingResolver;
import com.srijan.portfolio.email.TenantEmailContext;
import com.srijan.portfolio.entity.Contact;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.repository.ContactRepository;
import com.srijan.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactEmailService {

    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final EmailService emailService;
    private final TenantBrandingResolver tenantBrandingResolver;

    @Transactional(readOnly = true)
    public void sendPublicContactMessage(String username, ContactMessageRequest request) {
        String sanitizedUsername = sanitize(username);
        User owner = userRepository.findByUsername(sanitizedUsername)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        Contact contact = contactRepository.findByUserId(owner.getId()).orElse(null);
        String recipientEmail = firstNonBlank(
                contact != null ? sanitize(contact.getPrimaryEmail()) : null,
                sanitize(owner.getEmail())
        );

        if (recipientEmail == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CONTACT_EMAIL_UNAVAILABLE",
                    "This portfolio is not accepting email messages right now"
            );
        }

        TenantEmailContext tenantContext = tenantBrandingResolver.resolve(owner.getUsername());

        try {
            emailService.sendContactMessageEmail(
                    recipientEmail,
                    owner.getUsername(),
                    sanitize(request.getSubject()),
                    sanitize(request.getMessage()),
                    tenantContext
            );
        } catch (IllegalStateException exception) {
            log.warn("Failed to deliver public contact email username={} recipient={}", owner.getUsername(), recipientEmail, exception);
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "MAIL_DELIVERY_FAILED",
                    "Message could not be sent right now. Please try again shortly."
            );
        }
    }

    private String sanitize(String value) {
        return value == null ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
