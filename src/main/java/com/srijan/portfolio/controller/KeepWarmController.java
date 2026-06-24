package com.srijan.portfolio.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.srijan.portfolio.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class KeepWarmController {

    private static final Logger logger = LoggerFactory.getLogger(KeepWarmController.class);
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    @Autowired
    private UserRepository userRepository;

    private volatile long cachedCount;
    private volatile Instant cachedAt;

    /**
     * Populate the cache on startup so the first request
     * is always served from cache without special handling.
     */
    @PostConstruct
    void initCache() {
        try {
            cachedCount = userRepository.count();
            cachedAt = Instant.now();
            logger.info("User count cache initialized: count={}", cachedCount);
        } catch (Exception e) {
            cachedCount = 0;
            cachedAt = Instant.now();
            logger.warn("Failed to initialize user count cache, defaulting to 0", e);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> count() {
        long startTime = System.currentTimeMillis();
        boolean fromCache;

        if (isCacheExpired()) {
            // Cache has expired — attempt a fresh DB query
            try {
                long freshCount = userRepository.count();
                cachedCount = freshCount;
                cachedAt = Instant.now();
                fromCache = false;
                logger.info("User count cache refreshed: count={}", freshCount);
            } catch (Exception e) {
                // DB query failed — serve the stale cached value instead of erroring
                fromCache = true;
                logger.warn("Failed to refresh user count cache, serving stale value (count={})", cachedCount, e);
            }
        } else {
            // Cache is still valid — no DB call needed
            fromCache = true;
        }

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("status", "active");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        response.put("database_check", fromCache ? "cached" : "success");
        response.put("record_count", cachedCount);
        response.put("response_time_ms", duration);
        response.put("cached", fromCache);

        logger.info("User count request served in {}ms (cached={})", duration, fromCache);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        return ResponseEntity.ok(response);
    }

    private boolean isCacheExpired() {
        return cachedAt == null || Instant.now().isAfter(cachedAt.plus(CACHE_TTL));
    }
}
