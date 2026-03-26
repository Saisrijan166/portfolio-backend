package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.EmailOtp;
import com.srijan.portfolio.entity.EmailOtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    Optional<EmailOtp> findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(String email, EmailOtpPurpose purpose);

    List<EmailOtp> findAllByEmailIgnoreCaseAndPurposeAndUsedFalse(String email, EmailOtpPurpose purpose);
}
