package de.thm.swtp.api.tag.repository;

import de.thm.swtp.api.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repository for {@link Tag}.*/
public interface TagRepository extends JpaRepository<Tag, UUID> {

    /** Returns a list of all tags having the given name.*/
    List<Tag> findByName(String name);

    /** Returns a list of all tags whose name partially matches the given value. The search is case-insensitive. */
    List<Tag> findByNameContainingIgnoreCase(String name);

    /** Returns a list of all distinct tags from a project.*/
    List<Tag> findDistinctByProjectsIsNotEmpty();

    /** Returns a list of all distinct tags from a user-profile.*/
    List<Tag> findDistinctByUserProfilesIsNotEmpty();
}
