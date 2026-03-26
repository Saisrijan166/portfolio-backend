package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "auth_providers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_provider_user_provider", columnNames = {"user_id", "provider"}),
                @UniqueConstraint(name = "uk_auth_provider_provider_user", columnNames = {"provider", "provider_user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthProvider {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthProviderType provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
