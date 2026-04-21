package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_projects_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tenant Isolation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 60)
    private String name;

    @Column(length = 60)
    private String type; // e.g., "System Application"

    @Column(length = 60)
    private String status;

    @Column(length = 60)
    private String year;

    @Column(length = 1000)
    private String overview;

    // Storing tags/arrays as ElementCollections for simplicity
    @ElementCollection
    @CollectionTable(name = "project_tech_stacks", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "tech", length = 100)
    private List<String> techStack;

    // Links (live, source, pdf)
    @Column(length = 255)
    private String liveLink;

    @Column(length = 255)
    private String sourceLink;

    @Column(length = 255)
    private String pdfLink;

    // Media
    @Column(length = 255)
    private String mediaVideo;

    @ElementCollection
    @CollectionTable(name = "project_screenshots", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "screenshot_url", length = 255)
    private List<String> screenshots;

    private boolean isResearch; // Flag to separate generic projects from research papers

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
