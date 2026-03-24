package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.PortfolioFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioFeedbackRepository extends JpaRepository<PortfolioFeedback, Long> {
    Optional<PortfolioFeedback> findByOwnerIdAndVisitorTokenHash(Long ownerId, String visitorTokenHash);

    Optional<PortfolioFeedback> findFirstByOwnerIdAndVisitorTokenHash(Long ownerId, String visitorTokenHash);

    Page<PortfolioFeedback> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId, Pageable pageable);

    @Query("select coalesce(avg(p.rating), 0) from PortfolioFeedback p where p.owner.id = :ownerId")
    Double findAverageRatingByOwnerId(Long ownerId);

    long countByOwnerId(Long ownerId);
}
