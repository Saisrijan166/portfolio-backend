package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.AppearanceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppearanceConfigRepository extends JpaRepository<AppearanceConfig, Long> {
    Optional<AppearanceConfig> findByUserId(Long userId);
}
