package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String email;

    @Column
    private String passwordHash;

    @Column(nullable = false)
    private String role; // e.g. "ROLE_USER", "ROLE_ADMIN"

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isEmailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(16) default 'ACTIVE'")
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "active_template", nullable = false, length = 50)
    @Builder.Default
    private String activeTemplate = "linux-os";

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuthProvider> authProviders = new ArrayList<>();

    @Transient
    @Builder.Default
    private boolean newRegistration = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
