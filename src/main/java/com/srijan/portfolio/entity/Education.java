package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "educations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 20)
    private String level; // e.g., "Intermediate", "High School"

    @Column(length = 60)
    private String institute;

    @Column(length = 60)
    private String location;

    @Column(length = 60)
    private String degree; // "Science Stream", "ICSE Board"

    @Column(length = 20)
    private String scoreLabel; // "Percentage", "CGPA"

    @Column(length = 20)
    private String scoreValue; // "91%", "95%"

    @Column(length = 25)
    private String duration; // "2020 - 2022"

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
