package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "skills", indexes = {
    @Index(name = "idx_skills_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tenant Isolation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 30)
    private String domain; // "Frontend Engineering", "Backend & Systems"

    @Column(length = 30)
    private String name; // e.g. "React"
    private int level; // 1=Beginner, 2=Intermediate, 3=Advanced, 4=Expert

    private boolean isMetaSkill;
    @Column(length = 100)
    private String metaDescription; // For "System Thinking" meta skills

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
