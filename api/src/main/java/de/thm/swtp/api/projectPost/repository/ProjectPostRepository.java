package de.thm.swtp.api.projectPost.repository;

import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectPostRepository  extends JpaRepository<ProjectPostEntity, UUID> {

    /** Returns all post belonging to a project with the specified status. Ordered by published at*/
    List<ProjectPostEntity> findAllByProjectIdAndStatusOrderByPublishedAtDesc(UUID projectId, ProjectPostStatus status);

}
