package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "about_principles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AboutPrinciple {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "about_id", nullable = false)
    private About about;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(length = 30, nullable = false)
    private String title;

    @Column(length = 125, nullable = false)
    private String description;
}
