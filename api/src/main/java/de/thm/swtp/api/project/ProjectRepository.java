package de.thm.swtp.api.project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);

    Optional<ProjectEntity> findByProjectUrl(String projectUrl);

    /**
     * Finds all projects owned by the user with the given username that are not marked as deleted, ordered by creation date descending.
     *
     * @param username the username of the project owner
     * @return a list of ProjectEntity objects matching the criteria
     */
    List<ProjectEntity> findAllByOwnerUsernameAndDeletedAtIsNullOrderByCreatedAtDesc(String username);
}