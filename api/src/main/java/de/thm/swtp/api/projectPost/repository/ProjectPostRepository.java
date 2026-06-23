package de.thm.swtp.api.projectPost.repository;

import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectPostRepository  extends JpaRepository<ProjectPostEntity, UUID> {

    /** Returns all post belonging to a project with the specified status. Ordered by published at.*/
    List<ProjectPostEntity> findAllByProjectIdAndStatusOrderByPublishedAtDesc(UUID projectId, ProjectPostStatus status);

    /** Checks if project posts exists for a given project from a given user.*/
    boolean existsByIdAndProjectIdAndAuthorKeycloakId(UUID postId, UUID projectId, UUID authorKeycloakId);

    /** Returns the project post from a given project.*/
    Optional<ProjectPostEntity> findByIdAndProjectId(UUID postId, UUID projectId);
}
