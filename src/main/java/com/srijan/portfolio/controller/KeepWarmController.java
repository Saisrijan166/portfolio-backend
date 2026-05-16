package com.srijan.portfolio.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.srijan.portfolio.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class KeepWarmController {

    private static final Logger logger = LoggerFactory.getLogger(KeepWarmController.class);

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> count() {
        long startTime = System.currentTimeMillis();
        
        try {
            long recordCount = userRepository.count();
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "active");
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
            response.put("database_check", "success");
            response.put("record_count", recordCount);
            response.put("response_time_ms", duration);
            
            logger.info("Keep-warm check completed in {}ms", duration);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Keep-warm check failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(503).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        return ResponseEntity.ok(response);
    }
}
