package de.thm.swtp.api.thesis;

import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity(name = "theses")
@Table(name = "theses", uniqueConstraints = {
        @UniqueConstraint(name = "UK_theses_title",      columnNames = {"title"}),
        @UniqueConstraint(name = "UK_theses_thesis_url", columnNames = {"thesis_url"})
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThesisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "thesis_url", nullable = false, unique = true, length = 30)
    private String thesisUrl;

    @Column(length = 2000)
    private String description;

    @Column(name = "short_description", length = 300)
    private String shortDescription;

    @Column(name = "delete_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_keycloak_id", nullable = false)
    private UserProfile supervisor;

    @ManyToMany
    @JoinTable(
            name = "thesis_tags",
            joinColumns = @JoinColumn(name = "thesis_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_name")
    )
    @Builder.Default
    private Set<TagEntity> tags = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "thesis_students",
            joinColumns = @JoinColumn(name = "thesis_id"),
            inverseJoinColumns = @JoinColumn(name = "user_profile_keycloak_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "UK_thesis_students",
                    columnNames = {"thesis_id", "user_profile_keycloak_id"}
            )
    )
    @Builder.Default
    private Set<UserProfile> students = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
