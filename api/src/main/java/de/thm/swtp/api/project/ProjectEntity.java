package de.thm.swtp.api.project;


import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
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

    @Column(name="project_url", nullable = false, length = 30)
    private String projectUrl;

    @Column(name="is_private", nullable = false)
    private boolean isPrivateProject;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_keycloak_id", nullable = false)
    private UserProfile owner;


    @ManyToMany
    @JoinTable(name = "project_members", joinColumns = @JoinColumn(name = "project_id"), inverseJoinColumns = @JoinColumn(name = "user_profile_keycloak_id"))
    private List<UserProfile> members = new ArrayList<>();


    // Misses Join to TagEntity for project-tags. ManyToMany should work.
    @ManyToMany
    @JoinTable(name = "project_tags", joinColumns = @JoinColumn(name = "project_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<TagEntity> tags = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
