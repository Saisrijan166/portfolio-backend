package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "platform_feedback",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_platform_feedback_owner_visitor_source",
                        columnNames = {"portfolio_owner_user_id", "visitor_token_hash", "source_type"}
                ),
                @UniqueConstraint(
                        name = "uk_platform_feedback_submitter",
                        columnNames = {"submitted_by_user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_platform_feedback_owner_source_updated", columnList = "portfolio_owner_user_id, source_type, updated_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_owner_user_id")
    private User portfolioOwner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_user_id", unique = true)
    private User submittedByUser;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "visitor_token_hash", length = 64)
    private String visitorTokenHash;

    @Column(name = "submitter_name", length = 120)
    private String submitterName;

    @Column
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
