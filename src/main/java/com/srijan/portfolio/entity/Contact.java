package com.srijan.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 255)
    private String primaryEmail;

    @ElementCollection
    @CollectionTable(name = "contact_professional_links", joinColumns = @JoinColumn(name = "contact_id"))
    @AttributeOverrides({
            @AttributeOverride(name = "label", column = @Column(name = "label", length = 100)),
            @AttributeOverride(name = "url", column = @Column(name = "url", length = 512))
    })
    private List<ContactLink> professionalLinks;

    @ElementCollection
    @CollectionTable(name = "contact_social_links", joinColumns = @JoinColumn(name = "contact_id"))
    @AttributeOverrides({
            @AttributeOverride(name = "label", column = @Column(name = "label", length = 100)),
            @AttributeOverride(name = "url", column = @Column(name = "url", length = 512))
    })
    private List<ContactLink> socialLinks;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
