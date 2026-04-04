package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.CertificationAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationAchievementRepository extends JpaRepository<CertificationAchievement, Long> {
    List<CertificationAchievement> findByUserIdOrderByIdAsc(Long userId);

    List<CertificationAchievement> findByUserUsernameOrderByIdAsc(String username);

    Optional<CertificationAchievement> findByIdAndUserUsername(Long id, String username);
}
