package de.thm.swtp.api.tag.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** JPA entity representing a tag in the database.
 * Only used by the persistence layer and should never be exposed to the service or controller layer directly.
 * Tag-name is unique.
 */
@Entity
@Table(name="tags",
        uniqueConstraints = {@UniqueConstraint(name = "tag_name",columnNames = {"name"})},
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
    @Column(nullable = false,length = 30)
    private String name;


    // Relation for project and userprofile should be added in the ProjectEntity & UserprofileEntity
    // Bi-directional relations only needed when we want to get all projects or profiles with the requested tag. Currently not needed.
}
