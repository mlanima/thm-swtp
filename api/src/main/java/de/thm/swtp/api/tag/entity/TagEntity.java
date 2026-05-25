package de.thm.swtp.api.tag.entity;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JPA entity representing a tag in the database.
 * Only used by the persistence layer and should never be exposed to the service or controller layer directly.
 * Tag-name is unique.
 */
@Entity
@Table(name="tags",
        uniqueConstraints = {@UniqueConstraint(name = "tag_name", columnNames = {"name"})},
        indexes = {@Index(name="idx_tag_name", columnList = "name")})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagEntity {

    /** Unique identifier, auto-generated as UUID.*/
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Name of the tag.*/
    @NotBlank
    @Column(nullable = false, length = 30)
    private String name;


    /** List of all projects, where the tag is used.*/
    @ManyToMany(mappedBy = "tags")
    private List<ProjectEntity> projects = new ArrayList<>();

    /** List of all user profiles, where the tag is used.*/
    @ManyToMany(mappedBy = "tags")
    private List<UserProfile> userProfiles = new ArrayList<>();
}
