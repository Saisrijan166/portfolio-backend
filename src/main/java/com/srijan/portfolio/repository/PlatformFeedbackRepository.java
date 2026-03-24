package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.PlatformFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformFeedbackRepository extends JpaRepository<PlatformFeedback, Long> {
    Optional<PlatformFeedback> findBySubmittedByUserId(Long submittedByUserId);

    Optional<PlatformFeedback> findByPortfolioOwnerIdAndVisitorTokenHashAndSourceType(
            Long portfolioOwnerId,
            String visitorTokenHash,
            String sourceType
    );
}
