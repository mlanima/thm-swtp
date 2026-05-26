package de.thm.swtp.api.tag.repository;

import de.thm.swtp.api.tag.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repository for {@link TagEntity}.*/
public interface TagRepository extends JpaRepository<TagEntity, String> {

    /** Returns a list of all tags having the given name. Case-insensitive.*/
    Optional<TagEntity> findByNameIgnoreCase(String name);

    /** Returns a list of all tags whose name partially matches the given value. The search is case-insensitive. */
    List<TagEntity> findByNameContainingIgnoreCase(String name);

    /** Returns a list of all distinct tags from a project.*/
    List<TagEntity> findDistinctByProjectsIsNotEmpty();

    /** Returns a list of all distinct tags from a user-profile.*/
    List<TagEntity> findDistinctByUserProfilesIsNotEmpty();
}
