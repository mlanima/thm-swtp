package de.thm.swtp.api.tag.entity;

import de.thm.swtp.api.tag.domain.TagCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** JPA entity representing a tag in the database.
 * Only used by the persistence layer and should never be exposed to the service or controller layer directly.
 * Combination of 'name' and 'category' is unique.
 */
@Entity
@Table(name="tags",
        uniqueConstraints = {@UniqueConstraint(name = "tag_name_category",columnNames = {"name","category"})},
        indexes = {@Index(name="idx_tag_name", columnList = "name"), @Index(name = "idx_tag_category",columnList = "category")})
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

    /** Category of the tag.*/
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TagCategory category;

}
