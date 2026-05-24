package de.thm.swtp.api.tag.repository;

import de.thm.swtp.api.tag.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repository for {@link TagEntity}.*/
public interface TagRepository extends JpaRepository<TagEntity, UUID> {

    /** Returns a list of all tags having the given name.*/
    List<TagEntity> findByName(String name);

    /** Returns a list of all tags whose name partially matches the given value. The search is case-insensitive. */
    List<TagEntity> findByNameContainingIgnoreCase(String name);
}
