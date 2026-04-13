package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.ResumeAiCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeAiCacheRepository extends JpaRepository<ResumeAiCacheEntry, Long> {
    Optional<ResumeAiCacheEntry> findByCacheKey(String cacheKey);
}
