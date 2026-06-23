package de.thm.swtp.api.project;

import de.thm.swtp.api.projectInvitation.entity.ProjectInviteEntity;
import de.thm.swtp.api.links.entity.ProjectLinkEntity;
import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity(name = "projects")
@Table(name = "projects", uniqueConstraints = {
        @UniqueConstraint(name = "UK_projects_name",        columnNames = {"name"}),
        @UniqueConstraint(name = "UK_projects_project_url", columnNames = {"project_url"})
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "short_description", length = 200)
    private String shortDescription;

    @Column(name = "project_url", nullable = false, unique = true, length = 30)
    private String projectUrl;

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private boolean isPrivateProject = false;

    @Column(name = "allow_join_requests", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean allowJoinRequests = true;

    @Column(name = "open_positions_count", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int openPositionsCount = 0;

    @Column(name = "delete_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_keycloak_id", nullable = false)
    private UserProfile owner;

    @ManyToMany
    @JoinTable(
            name = "project_members",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_profile_keycloak_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "UK_project_members",
                    columnNames = {"project_id", "user_profile_keycloak_id"}
            )
    )
    @Builder.Default
    private Set<UserProfile> members = new HashSet<>();

    // Misses Join to TagEntity for project-tags. ManyToMany should work.
    @ManyToMany
    @JoinTable(
            name = "project_tags",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_name")
    )
    @Builder.Default
    private Set<TagEntity> tags = new HashSet<>();

    @OneToMany(mappedBy = "project", orphanRemoval = true)
    @Builder.Default
    private List<ProjectInviteEntity> invitations = new ArrayList<>();

    @OneToMany(mappedBy = "project", orphanRemoval = true)
    @Builder.Default
    private List<ProjectLinkEntity> links = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}