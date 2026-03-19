package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "experiences", indexes = {
    @Index(name = "idx_experiences_user_id", columnList = "user_id"),
    @Index(name = "idx_experiences_user_deleted", columnList = "user_id,deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tenant Isolation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String company;
    private String roleTitle;
    private String duration; // e.g. "Jan 2023 - Present"

    private Integer startYear;
    private Integer endYear;
    private boolean isCurrent;

    @ElementCollection
    @CollectionTable(name = "experience_responsibilities", joinColumns = @JoinColumn(name = "experience_id"))
    @Column(name = "responsibility", length = 1000)
    private List<String> responsibilities;

    @ElementCollection
    @CollectionTable(name = "experience_achievements", joinColumns = @JoinColumn(name = "experience_id"))
    @Column(name = "achievement", length = 1000)
    private List<String> achievements;

    @ElementCollection
    @CollectionTable(name = "experience_skills", joinColumns = @JoinColumn(name = "experience_id"))
    @Column(name = "skill")
    private List<String> skills;

    // Flag to separate "Professional Experience" vs "Academic Journey"
    private boolean isAcademic;

    // Academic specific fields
    private String level;
    private String institute;
    private String location;
    private String degree;
    private String scoreLabel; // e.g. "GPA", "Percentage"
    private String scoreValue;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
