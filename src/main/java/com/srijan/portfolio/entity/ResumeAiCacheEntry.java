package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_ai_cache_entries", indexes = {
        @Index(name = "idx_resume_ai_cache_key", columnList = "cacheKey", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAiCacheEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128, unique = true)
    private String cacheKey;

    @Column(nullable = false, length = 64)
    private String cacheType;

    @Lob
    @Column(nullable = false)
    private String responseJson;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
