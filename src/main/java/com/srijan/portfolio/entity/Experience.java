package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "experiences", indexes = {
    @Index(name = "idx_experiences_user_id", columnList = "user_id")
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

    @Column(length = 60)
    private String company;

    @Column(length = 60)
    private String roleTitle;

    @Column(length = 60)
    private String duration; // e.g. "Jan 2023 - Present"

    private Integer startMonth;

    private Integer startYear;
    private Integer endMonth;
    private Integer endYear;
    private boolean isCurrent;

    @ElementCollection
    @CollectionTable(name = "experience_responsibilities", joinColumns = @JoinColumn(name = "experience_id"))
    @Column(name = "responsibility", length = 600)
    private List<String> responsibilities;

    @ElementCollection
    @CollectionTable(name = "experience_achievements", joinColumns = @JoinColumn(name = "experience_id"))
    @Column(name = "achievement", length = 600)
    private List<String> achievements;

    @ElementCollection
    @CollectionTable(name = "experience_skills", joinColumns = @JoinColumn(name = "experience_id"))
    @Column(name = "skill", length = 20)
    private List<String> skills;

    // Flag to separate "Professional Experience" vs "Academic Journey"
    private boolean isAcademic;

    // Academic specific fields
    @Column(length = 60)
    private String level;

    @Column(length = 60)
    private String institute;

    @Column(length = 60)
    private String location;

    @Column(length = 60)
    private String degree;

    @Column(length = 60)
    private String scoreLabel; // e.g. "GPA", "Percentage"

    @Column(length = 60)
    private String scoreValue;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
