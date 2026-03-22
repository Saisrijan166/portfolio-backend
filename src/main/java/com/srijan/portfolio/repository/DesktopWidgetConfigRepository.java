package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.DesktopWidgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DesktopWidgetConfigRepository extends JpaRepository<DesktopWidgetConfig, Long> {
    Optional<DesktopWidgetConfig> findByUserId(Long userId);
}
