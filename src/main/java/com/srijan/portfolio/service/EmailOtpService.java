package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.OtpRequestDto;
import com.srijan.portfolio.entity.EmailOtp;
import com.srijan.portfolio.entity.EmailOtpPurpose;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.exception.UnauthorizedException;
import com.srijan.portfolio.repository.EmailOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private final EmailOtpRepository emailOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final AuthSupportService authSupportService;

    private final SecureRandom random = new SecureRandom();

    @Value("${auth.otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${auth.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${auth.otp.resend-cooldown-seconds:45}")
    private int resendCooldownSeconds;

    @Value("${auth.otp.mail-from:no-reply@portfolio.local}")
    private String mailFrom;

    @Transactional
    public void sendLoginOtp(OtpRequestDto request) {
        String normalizedEmail = authSupportService.normalizeEmail(request.getEmail());
        EmailOtp latest = emailOtpRepository
                .findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(normalizedEmail, EmailOtpPurpose.LOGIN)
                .orElse(null);

        if (latest != null && latest.getCreatedAt() != null
                && latest.getCreatedAt().plusSeconds(resendCooldownSeconds).isAfter(java.time.LocalDateTime.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RESEND_BLOCKED", "Please wait before requesting another OTP");
        }

        invalidateActiveOtps(normalizedEmail, EmailOtpPurpose.LOGIN);

        String otp = "%06d".formatted(random.nextInt(1_000_000));
        EmailOtp entity = EmailOtp.builder()
                .email(normalizedEmail)
                .otpHash(passwordEncoder.encode(otp))
                .purpose(EmailOtpPurpose.LOGIN)
                .expiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES))
                .attempts(0)
                .used(false)
                .build();
        emailOtpRepository.save(entity);

        sendOtpMail(normalizedEmail, otp);
    }

    @Transactional
    public String verifyLoginOtp(String email, String otp) {
        String normalizedEmail = authSupportService.normalizeEmail(email);
        EmailOtp current = emailOtpRepository
                .findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(normalizedEmail, EmailOtpPurpose.LOGIN)
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

    private void sendOtpMail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(mailFrom);
        message.setSubject("Your login OTP");
        message.setText("Your verification code is " + otp + ". It expires in " + expiryMinutes + " minutes.");
        mailSender.send(message);
    }
}
