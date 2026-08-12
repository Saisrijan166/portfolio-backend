package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.OtpRequestDto;
import com.srijan.portfolio.dto.PasswordResetConfirmRequest;
import com.srijan.portfolio.dto.PasswordResetRequest;
import com.srijan.portfolio.email.EmailService;
import com.srijan.portfolio.entity.EmailOtp;
import com.srijan.portfolio.entity.EmailOtpPurpose;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.exception.UnauthorizedException;
import com.srijan.portfolio.repository.EmailOtpRepository;
import com.srijan.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private final EmailOtpRepository emailOtpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthSupportService authSupportService;
    private final RefreshTokenService refreshTokenService;
    private final com.srijan.portfolio.email.TenantBrandingResolver tenantBrandingResolver;

    private final SecureRandom random = new SecureRandom();

    @Value("${auth.otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${auth.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${auth.otp.resend-cooldown-seconds:45}")
    private int resendCooldownSeconds;

    @Transactional
    public void sendLoginOtp(OtpRequestDto request) {
        String normalizedEmail = authSupportService.normalizeEmail(request.getEmail());
        issueOtp(normalizedEmail, EmailOtpPurpose.LOGIN);
    }

    @Transactional
    public String verifyLoginOtp(String email, String otp) {
        return verifyOtp(email, otp, EmailOtpPurpose.LOGIN);
    }

    @Transactional
    public void sendPasswordResetOtp(PasswordResetRequest request) {
        String normalizedEmail = authSupportService.normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) {
            log.info("Password reset requested for non-existent email={}", normalizedEmail);
            return;
        }

        issueOtp(normalizedEmail, EmailOtpPurpose.RESET_PASSWORD);
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        String normalizedEmail = verifyOtp(request.getEmail(), request.getOtp(), EmailOtpPurpose.RESET_PASSWORD);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("PASSWORD_RESET_UNAVAILABLE", "Password reset is unavailable"));

        if (user.getPasswordHash() != null && passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.CONFLICT, "PASSWORD_UNCHANGED", "New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setEmailVerified(true);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());

        try {
            emailService.sendPasswordChangedEmail(user.getEmail(), true, null);
        } catch (Exception exception) {
            log.warn("Failed to send password reset success email to={}", normalizedEmail, exception);
        }
    }

    private void issueOtp(String email, EmailOtpPurpose purpose) {
        assertCooldown(email, purpose);
        invalidateActiveOtps(email, purpose);

        String otp = "%06d".formatted(random.nextInt(1_000_000));
        EmailOtp entity = EmailOtp.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(otp))
                .purpose(purpose)
                .expiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES))
                .attempts(0)
                .used(false)
                .build();
        emailOtpRepository.save(entity);

        sendOtpMail(email, otp, purpose);
    }

    private void assertCooldown(String email, EmailOtpPurpose purpose) {
        EmailOtp latest = emailOtpRepository
                .findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElse(null);

        if (latest != null && latest.getCreatedAt() != null
                && latest.getCreatedAt().plusSeconds(resendCooldownSeconds).isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RESEND_BLOCKED", "Please wait before requesting another OTP");
        }
    }

    private String verifyOtp(String email, String otp, EmailOtpPurpose purpose) {
        String normalizedEmail = authSupportService.normalizeEmail(email);
        EmailOtp current = emailOtpRepository
                .findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(normalizedEmail, purpose)
                .orElseThrow(() -> new UnauthorizedException("OTP_NOT_FOUND", "OTP not found or expired"));

        if (current.isUsed()) {
            throw new UnauthorizedException("OTP_ALREADY_USED", "OTP has already been used");
        }
        if (current.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("OTP_EXPIRED", "OTP has expired");
        }
        if (current.getAttempts() >= maxAttempts) {
            throw new UnauthorizedException("OTP_ATTEMPTS_EXCEEDED", "OTP verification attempts exceeded");
        }

        current.setAttempts(current.getAttempts() + 1);
        if (!passwordEncoder.matches(otp, current.getOtpHash())) {
            emailOtpRepository.save(current);
            throw new UnauthorizedException("OTP_INVALID", "OTP is invalid");
        }

        current.setUsed(true);
        emailOtpRepository.save(current);
        return normalizedEmail;
    }

    private void invalidateActiveOtps(String email, EmailOtpPurpose purpose) {
        List<EmailOtp> active = emailOtpRepository.findAllByEmailIgnoreCaseAndPurposeAndUsedFalse(email, purpose);
        for (EmailOtp otp : active) {
            otp.setUsed(true);
        }
    }

    private void sendOtpMail(String email, String otp, EmailOtpPurpose purpose) {
        com.srijan.portfolio.email.TenantEmailContext tenantContext = userRepository.findByEmailIgnoreCase(email)
                .map(u -> tenantBrandingResolver.resolve(u.getUsername()))
                .orElse(null);

        try {
            if (purpose == EmailOtpPurpose.RESET_PASSWORD) {
                emailService.sendOtpEmail(
                        email,
                        otp,
                        expiryMinutes,
                        "Reset Your Password",
                        "Password reset code",
                        "Use the code below to reset your password and continue securely.",
                        tenantContext
                );
                return;
            }

            emailService.sendOtpEmail(
                    email,
                    otp,
                    expiryMinutes,
                    "Your Login Verification Code",
                    "Login verification code",
                    "Use the code below to continue.",
                    tenantContext
            );
        } catch (Exception exception) {
            log.warn("Failed to send OTP email purpose={} to={}", purpose, email, exception.getCause() != null ? exception.getCause() : exception);
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OTP_DELIVERY_FAILED",
                    "We couldn't send your verification code right now. Please try again in a few moments."
            );
        }
    }
}
