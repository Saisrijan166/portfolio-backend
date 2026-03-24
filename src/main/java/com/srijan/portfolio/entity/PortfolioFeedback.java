package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "portfolio_feedback",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_portfolio_feedback_owner_visitor",
                        columnNames = {"owner_user_id", "visitor_token_hash"}
                )
        },
        indexes = {
                @Index(name = "idx_portfolio_feedback_owner_updated", columnList = "owner_user_id, updated_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "visitor_token_hash", nullable = false, length = 64)
    private String visitorTokenHash;

    @Column(name = "visitor_ip", length = 128)
    private String visitorIp;

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
