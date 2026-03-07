package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tenant Isolation
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String name;
    private String roleTitle;

    @Column(length = 2000)
    private String bio;
    private String image; // URL to the profile picture
    private String location;
    private String availability;
    private String experienceYears;

    @Column(columnDefinition = "TEXT")
    private String about;

    private String osName;
    private String accountType;
    private String access;
    private String roleDescription;

    @Column(columnDefinition = "TEXT")
    private String principles; // Store as JSON array of principles

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
