package de.thm.swtp.api.projectLinks.repository;

import de.thm.swtp.api.projectLinks.entity.ProjectLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectLinkRepository extends JpaRepository<ProjectLinkEntity, UUID> {

    List<ProjectLinkEntity> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    boolean existsByProjectIdAndUrlIgnoreCase(UUID projectId, String url);

}
