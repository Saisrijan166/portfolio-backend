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

    // Legacy shared fields retained for backwards-compatible reads.
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

    // About module fields
    private String aboutName;
    private String aboutRoleTitle;

    @Column(length = 2000)
    private String aboutBio;
    private String aboutImage;
    private String aboutLocation;
    private String aboutAvailability;
    private String aboutExperienceYears;

    // Profile module fields
    private String profileName;
    private String profileRoleTitle;
    private String profileLocation;
    private String profileAvailability;
    private String profilePrimaryEmail;

    @Column(columnDefinition = "TEXT")
    private String profileProfessionalLinks;

    @Column(columnDefinition = "TEXT")
    private String profileSocialLinks;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
