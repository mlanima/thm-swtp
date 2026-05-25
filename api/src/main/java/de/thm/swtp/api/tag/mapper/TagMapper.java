package de.thm.swtp.api.tag.mapper;

import de.thm.swtp.api.tag.domain.Tag;
import de.thm.swtp.api.tag.entity.Tag;

/** Maps between {@link Tag} and {@link Tag} domain objects. */
public class TagMapper {

    /** Converts the entity into a domain object including all its information.*/
    public static Tag toDomain(Tag entity) {
        return Tag.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
