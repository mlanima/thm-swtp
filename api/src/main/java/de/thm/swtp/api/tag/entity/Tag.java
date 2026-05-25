package de.thm.swtp.api.tag.entity;

import de.thm.swtp.api.project.Project;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

/** JPA entity representing a tag in the database.
 * Only used by the persistence layer and should never be exposed to the service or controller layer directly.
 * Tag-name is unique.
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tag {
    /** Name of the tag.*/
    @Id
    @Column(nullable = false, length = 30)
    private String name;


    /** List of all projects, where the tag is used.*/
    @ManyToMany(mappedBy = "tags")
    private Set<Project> projects;

    /** List of all user profiles, where the tag is used.*/
    @ManyToMany(mappedBy = "tags")
    private Set<UserProfile> userProfiles;

    public Tag(String name) {
        this.name = name;
    }
}
