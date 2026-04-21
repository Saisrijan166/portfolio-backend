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

    @Column(length = 60)
    private String osName;

    @Column(length = 60)
    private String accountType;

    @Column(length = 60)
    private String access;

    @Column(length = 60)
    private String roleDescription;

    // Profile module fields
    @Column(length = 60)
    private String profileName;

    @Column(length = 60)
    private String profileRoleTitle;

    @Column(length = 60)
    private String profileLocation;

    @Column(length = 60)
    private String profileAvailability;

    @Column(length = 80)
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
