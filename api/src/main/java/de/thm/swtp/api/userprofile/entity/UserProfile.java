package de.thm.swtp.api.userprofile.entity;

import de.thm.swtp.api.tag.entity.TagEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID keycloakId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String email;

    private String title;

    private String location;

    // TODO: followers is not implemented yet — placeholder until a follow system is built
    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private int followers;

    @Column(columnDefinition = "TEXT")
    private String about;

    @Column(columnDefinition = "TEXT")
    private String experience;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(name = "user_profile_tags", joinColumns = @JoinColumn(name = "user_profile_keycloak_id"), inverseJoinColumns = @JoinColumn(name = "tag_name"))
    private Set<TagEntity> tags = new HashSet<>();
}
