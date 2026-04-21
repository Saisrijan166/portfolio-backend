package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "abouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class About {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 60)
    private String name;

    @Column(length = 60)
    private String roleTitle;

    @Column(length = 500)
    private String bio;

    @Column(length = 255)
    private String image;

    @Column(length = 60)
    private String location;

    @Column(length = 60)
    private String availability;

    @Column(length = 60)
    private String experienceYears;

    @Column(columnDefinition = "TEXT")
    private String aboutContent;

    @Builder.Default
    @OneToMany(mappedBy = "about", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<AboutPrinciple> principles = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
